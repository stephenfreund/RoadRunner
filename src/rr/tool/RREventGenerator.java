/******************************************************************************

Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
                    and Stephen Freund (Williams College) 

All rights reserved.  

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

 * Neither the names of the University of California, Santa Cruz
      and Williams College nor the names of its contributors may be
      used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 ******************************************************************************/

package rr.tool;

import rr.RRMain;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassAccessedEvent;
import rr.event.ClassInitializedEvent;
import rr.event.FieldAccessEvent;
import rr.event.InterruptEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.loader.Loader;
import rr.loader.LoaderContext;
import rr.meta.ArrayAccessInfo;
import rr.meta.ClassInfo;
import rr.meta.FieldAccessInfo;
import rr.meta.FieldInfo;
import rr.meta.InterruptInfo;
import rr.meta.InvokeInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.meta.StartInfo;
import rr.state.AbstractArrayState;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.ShadowVolatile;
import rr.state.update.AbstractFieldUpdater;
import acme.util.Assert;
import acme.util.AtomicFlag;
import acme.util.ResourceManager;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public class RREventGenerator extends RR {


	public static CommandLineOption<Boolean> trackReentrantOption = 
			CommandLine.makeBoolean("reentrantEvents", false, CommandLineOption.Kind.EXPERIMENTAL, "Notify on reentrant lock ops.");

	public static CommandLineOption<Boolean> noJoinOption  = 
			CommandLine.makeBoolean("nojoin", false, CommandLineOption.Kind.EXPERIMENTAL, "By default RoadRunner waits for a thread to finishin by joining on it.  This causes problems if the target wait()'s on a Thread object, as is the case in Eclipse.  This option turns on a less efficient polling scheme.");

	public static CommandLineOption<Boolean> multiClassLoaderOption  = 
			CommandLine.makeBoolean("multiLoader", false, CommandLineOption.Kind.EXPERIMENTAL, "Attempt to support multiple class loaders.");


	public final static CommandLineOption<Integer> indicesToWatch  = 
			CommandLine.makeInteger("indices", Integer.MAX_VALUE, CommandLineOption.Kind.EXPERIMENTAL, "Specifies max array index to watch", new Runnable() {
				public void run() {
					maxArrayIndex = indicesToWatch.get();
				}	
			});

	protected static int maxArrayIndex = Integer.MAX_VALUE;

	protected static boolean matches(final int index) {
		return true || index <= maxArrayIndex;
	} 



	/****************************************************************/


	protected static FieldAccessEvent prepAccessEvent(Object target, ShadowVar gs, int fadId, ShadowThread td, boolean isWrite) {
		FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(fadId);
		AbstractFieldUpdater updater;

		// must be done first!  because loading updater could trigget other accesses,
		// which will write over fields of fae.  Bitter...
		updater = fad.getField().getUpdater();  

		FieldAccessEvent fae = td.getFieldAccessEvent();
		fae.setTarget(target);
		fae.setInfo(fad);
		fae.setUpdater(updater);
		fae.setWrite(isWrite);

		if (gs == null) {
			fae.putOriginalShadow(null);
			gs = getTool().makeShadowVar(fae);
			Assert.assertTrue(gs != null);
			if (!updater.putState(target, null, gs)) {
				Yikes.yikes("concurrent initialization of guard state");
				gs = updater.getState(target);
				if (gs == null) Assert.fail("concurrent updates to new var state not resolved properly: " + fae + " " + fae.getShadow());
			}
		}

		fae.putOriginalShadow(gs);
		return fae;
	}	


	public static void readAccess(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			FieldAccessEvent ae = prepAccessEvent(target, gs, fadId, td, false);
			firstAccess.access(ae);	
			ae.setInfo(null);
			ae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void writeAccess(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			FieldAccessEvent ae = prepAccessEvent(target, gs, fadId, td, true);
			firstAccess.access(ae);
			ae.setInfo(null);
			ae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}


	protected static VolatileAccessEvent prepVolatileAccessEvent(Object target,
			ShadowVar gs, int fadId, ShadowThread td, boolean isWrite) {
		FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(fadId);
		// do first.  see above

		AbstractFieldUpdater updater;

		// must be done first!  because loading updater could trigget other accesses,
		// which will write over fields of fae.  Bitter...
		updater = fad.getField().getUpdater();  

		VolatileAccessEvent fae = td.getVolatileAccessEvent();
		fae.setTarget(target);
		fae.setInfo(fad);
		fae.setUpdater(updater);
		fae.setWrite(isWrite);
		fae.setShadowVolatile(ShadowVolatile.get(target, fad.getField()));
		if (gs == null) { 
			fae.putOriginalShadow(null);
			gs = getTool().makeShadowVar(fae);
			if (!fae.putShadow(gs)) {
				gs = updater.getState(target);
				Assert.assertTrue(gs != null, "concurrent updates to new var state not resolved properly");
			}
		}

		fae.putOriginalShadow(gs);
		return fae;
	}

	public static void volatileWriteAccess(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			VolatileAccessEvent ae = prepVolatileAccessEvent(target, gs, fadId, td, true);
			getTool().volatileAccess(ae);
			ae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void volatileReadAccess(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			VolatileAccessEvent ae = prepVolatileAccessEvent(target, gs, fadId, td, false);
			getTool().volatileAccess(ae);
			ae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}


	/****/

	public static ShadowVar cloneVariableState(ShadowVar shadowVar) {
		if (shadowVar != null) {
			shadowVar = getTool().cloneState(shadowVar);
		}
		return shadowVar;
	}

	/****/

	public static void acquire(Object lock, int lockAcquireId, ShadowThread td) { 
		try {
			ShadowLock ld = td.acquire(lock);
			if (ld != null) {
				AcquireEvent ae = td.getAcquireEvent();
				ae.setInfo(MetaDataInfoMaps.getAcquires().get(lockAcquireId));
				ae.setLock(ld);

				firstAcquire.acquire(ae); 
				ae.setLock(null);
			} 
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void release(Object lock, int lockReleaseId, ShadowThread td) {
		try {
			ShadowLock ld = td.release(lock);
			if (ld != null) {
				ReleaseEvent re = td.getReleaseEvent();
				re.setLock(ld);
				re.setInfo(MetaDataInfoMaps.getReleases().get(lockReleaseId));
				firstRelease.release(re);
			}
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	/*
	 * return true to actually perform acquire.
	 */
	public static boolean testAcquire(Object lock, int lockAcquireId, ShadowThread td) {
		try { 
			ShadowLock ld = ShadowLock.get(lock);
			if (ld.get() > 0 && ld.getHoldingThread() == td) return true; //nested locking

			AcquireEvent ae = td.getAcquireEvent();
			ae.setInfo(MetaDataInfoMaps.getAcquires().get(lockAcquireId));
			ae.setLock(ld);

			return getTool().testAcquire(ae);
		} catch(Throwable e) {
			Assert.panic(e);
			return false;  
		}
	}

	/*
	 * return true to actually perform release.
	 */
	public static boolean testRelease(Object lock, int lockReleaseId, ShadowThread td) {
		try {
			ShadowLock ld = ShadowLock.get(lock);
			if (ld.get() > 1) return true; //nested unlocking;

			ReleaseEvent re = td.getReleaseEvent();
			re.setLock(ld);
			re.setInfo(MetaDataInfoMaps.getReleases().get(lockReleaseId));

			return getTool().testRelease(re);
		} catch(Throwable e) {
			Assert.panic(e);
			return false;
		}
	}

	/*****/

	public static void start(final Thread t, int startId) {
		start(t, startId, ShadowThread.getCurrentShadowThread());
	}

	public static void start(final Thread t, int startId, ShadowThread td) {
		start(ShadowThread.make(t, td), startId, td);
	}

	public static void start(final ShadowThread newTD, int startId, ShadowThread td) {
		try {
			final Thread t = newTD.getThread();
			StartInfo info = MetaDataInfoMaps.getStarts().get(startId);

			StartEvent se = td.getStartEvent();
			se.setNewThread(newTD);
			se.setInfo(info);
			getTool().preStart(se);

			Thread stopper = new Thread("RR Waiter for " + newTD.getTid()) {
				@Override
				public void run() {
					try {
						RRMain.incThreads();
						if (noJoinOption.get()) {
							while (true) {
								State s = t.getState();
								if (s == State.TERMINATED) break;
								Thread.sleep(500);
							}
						} else {
							t.join();
						}
						newTD.terminate();
						RRMain.decThreads();
					} catch (Exception e) {
						Assert.panic(e);
					}
				}
			};
			stopper.setDaemon(true);

			t.start();
			stopper.start();
			getTool().postStart(se);
		} catch(Throwable e) {
			Assert.panic(e);
		}
	}

	public static void join(Thread thread, int joinId) {
		join(thread, 0, 0, joinId);
	}

	public static void join(Thread thread, long millis, int joinId) {
		join(thread, millis, 0, joinId);
	}

	public static void join(Thread thread, long millis, int nanos, int joinId) {
		ShadowThread currentThread = ShadowThread.getCurrentShadowThread();
		join(thread, millis, nanos, joinId, currentThread);
	}


	public static void join(Thread thread, long millis, int nanos, int joinId, ShadowThread td) {
		while (true) {
			if (thread != null) break;
			Yikes.yikes("Join before start.  Retry...");				
		}
		join(ShadowThread.getShadowThread(thread), millis, nanos, joinId, td);
	}

	public static void join(ShadowThread thread, long millis, int nanos, int joinId, ShadowThread td) {
		try {
			JoinEvent je = td.getJoinEvent();
			je.setJoiningThread(thread);
			je.setInfo(MetaDataInfoMaps.getJoins().get(joinId));

			getTool().preJoin(je);
			je.getJoiningThread().getIsStopped().waitUntilTrue();
			getTool().postJoin(je);
		} catch(Throwable e) {
			Assert.panic(e);
		}

	}

	/*****/

	public static void wait(Object lock, int waitDataId) throws InterruptedException {
		wait(lock, 0, 0, waitDataId);
	}

	public static void wait(Object lock, long n, int waitDataId) throws InterruptedException {
		wait(lock, n, 0, waitDataId);
	}

	public static void wait(Object lock, long ms, int ns, int waitDataId) throws InterruptedException {
		ShadowThread td = ShadowThread.getCurrentShadowThread();
		wait(lock, ms, ns, waitDataId, td);
	}

	public static void wait(Object lock, long ms, int ns, int waitDataId, ShadowThread td) throws InterruptedException {
		try {
			ShadowLock ld = ShadowLock.get(lock);

			WaitEvent we = td.getWaitEvent();
			we.setInfo(MetaDataInfoMaps.getWaits().get(waitDataId));
			we.setLock(ld);

			int oldAcquireCount = ld.get();  // how many times we have acquired lock

			getTool().preWait(we);
			ld.set(0, null); // release lock
			synchronized(lock) {  // just in case...
				lock.wait(ms, ns);
			}
			Assert.assertTrue(ld.get() == 0);  // make sure no one is holding the lock.
			ld.set(oldAcquireCount, td);  // restore to previous state.
			getTool().postWait(we);
		} catch(InterruptedException e) {
			throw e;
		} catch(Throwable e) {
			Assert.panic(e);
		}
	}

	public static void notify(Object lock) {
		notify(lock, false, ShadowThread.getCurrentShadowThread());
	}

	public static void notify(Object lock, ShadowThread td) {
		notify(lock, false, td);
	}

	public static void notifyAll(Object lock) {
		notify(lock, true, ShadowThread.getCurrentShadowThread());
	}

	public static void notifyAll(Object lock, ShadowThread td) {
		notify(lock, true, td);
	}


	protected static void notify(Object lock, boolean all, ShadowThread td) {
		try {
			NotifyEvent ne = td.getNotifyEvent();
			ne.setLock(ShadowLock.get(lock));
			ne.setNotifyAll(all);

			getTool().preNotify(ne);
			synchronized(lock) {
				if (all) {
					lock.notifyAll();
				} else {
					lock.notify();
				}
			}
			getTool().postNotify(ne);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void sleep(long ms) throws InterruptedException {
		sleep(ms, 0);
	}

	public static void sleep(long ms, int ns) throws InterruptedException {
		sleep (ms, ns, ShadowThread.getCurrentShadowThread());
	}

	public static void sleep(long ms, int ns, ShadowThread td) throws InterruptedException {

		try {
			SleepEvent sleepEvent = td.getSleepEvent();
			getTool().preSleep(sleepEvent);
			Thread.sleep(ms, ns);
			getTool().postSleep(sleepEvent);
		} catch(InterruptedException e) {
			throw e;
		} catch(Throwable e) {
			Assert.panic(e);
		}
	}


	public static void invoke(int invokeId, ShadowThread td) { 
		try {
			td.invokeId = invokeId;
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void enter(final Object target, final int methodDataId, final ShadowThread td) {

		try {
			final MethodInfo methodData = MetaDataInfoMaps.getMethods().get(methodDataId);
			final MethodEvent me = td.enter(target, methodData); 
			int invokeId = td.invokeId;
			try {
				final InvokeInfo invokeInfo = MetaDataInfoMaps.getInvokes().get(invokeId);
				me.setInvokeInfo(invokeInfo);
			} catch (Exception e) {
				if (invokeId != -1) throw e;  // hack to ignore initial call to main when invokeId will be -1...
			}
			firstEnter.enter(me);
		} catch(Throwable e) {
			Assert.panic(e);
		}
	}

	public static void exit(ShadowThread td) {
		try {
			final MethodEvent me = td.getBlock(td.getBlockDepth() - 1);
			me.setEnter(false);
			firstExit.exit(me);
			td.exit();
		} catch(Throwable e) {
			Assert.panic(e);
		}
	}


	public static AbstractArrayState arrayShadow(final Object array, final int index, final int arrayAccessId, final ShadowThread td) {
		final ArrayAccessInfo aad = MetaDataInfoMaps.getArrayAccesses().get(arrayAccessId);
		return aad.getCache().get(array, td);
	}

	public static void arrayRead(Object array, int index, int arrayAccessId, ShadowThread td) {
		final AbstractArrayState as = arrayShadow(array, index, arrayAccessId, td);

		arrayRead(array, index, arrayAccessId, td, as);
	}

	public static void arrayRead(Object array, int index, int arrayAccessId, ShadowThread td, AbstractArrayState as) {
		try {
			if (!matches(index)) return;

			final ArrayAccessEvent aae = prepArrayAccessEvent(array, index,
					arrayAccessId, td, as, false);

			firstAccess.access(aae);
			aae.setTarget(null);

		} catch (Throwable e) {
			Assert.panic(e);
		}
	}


	protected static ArrayAccessEvent prepArrayAccessEvent(Object array,
			int index, int arrayAccessId, ShadowThread td, AbstractArrayState as, boolean isWrite) {
		final ArrayAccessInfo aad = MetaDataInfoMaps.getArrayAccesses().get(arrayAccessId); 
		ArrayAccessEvent aae = td.getArrayAccessEvent();

		aae.setIndex(index);
		aae.setWrite(isWrite);
		aae.setTarget(array);
		aae.setInfo(aad);
		aae.setArrayState(as);

		ShadowVar gs = as.getState(index);
		if (gs == null) {
			aae.putOriginalShadow(null);
			gs = getTool().makeShadowVar(aae);
			if (!aae.putShadow(gs)) {
				Yikes.yikes("Concurrent array guard state init...");
				gs = as.getState(index);
				Assert.assertTrue(gs != null, "concurrent updates to new var state not resolved properly.");
			}
		} 
		aae.putOriginalShadow(gs);
		return aae;
	}

	public static void arrayWrite(Object array, int index, int arrayAccessId, ShadowThread td) {
		final AbstractArrayState as = arrayShadow(array, index, arrayAccessId, td);
		arrayWrite(array, index, arrayAccessId, td, as);
	}

	public static void arrayWrite(Object array, int index, int arrayAccessId, ShadowThread td, AbstractArrayState as) {
		try { 
			if (!matches(index)) return;
			ArrayAccessEvent aae = prepArrayAccessEvent(array, index,
					arrayAccessId, td, as, true);
			firstAccess.access(aae);
			aae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void interrupt(Thread target, final int interruptId) {
		try {
			ShadowThread td = ShadowThread.getCurrentShadowThread();
			final InterruptInfo data = MetaDataInfoMaps.getInterrupts().get(interruptId);
			final InterruptEvent me = td.getInterruptEvent();
			me.setInterruptedThread(ShadowThread.getShadowThread(target));
			me.setInfo(data);
			getTool().preInterrupt(me);
			target.interrupt();
		} catch(Throwable e) {
			Assert.panic(e);
		}
	}


	public static boolean isAlive(Thread t, int joinId) {
		try {
			if (!t.isAlive()) {
				ShadowThread td = ShadowThread.getCurrentShadowThread();
				JoinEvent je = td.getJoinEvent();
				final ShadowThread shadowThread = ShadowThread.getShadowThread(t);
				if (shadowThread != null) {
					//				Util.assertTrue(shadowThread != null);
					je.setJoiningThread(shadowThread);
					je.setInfo(MetaDataInfoMaps.getJoins().get(joinId));
					getTool().preJoin(je);
					getTool().postJoin(je);
				}
				return false;
			}
		} catch(Throwable e) {
			Assert.panic(e);
		}
		return true;
	}


	public static void classInitEvent(String className) {
		try {
			ClassInfo c = MetaDataInfoMaps.getClass(className);
			ShadowThread td = ShadowThread.getCurrentShadowThread();
			ClassInitializedEvent e = td.getClassInitEvent();
			e.setRRClass(c);
			getTool().classInitialized(e);
		} catch (Throwable e) {
			Assert.panic(e);
		}

	}

	public static void classAccessEvent(ClassInfo c, ShadowThread td) {
		try {
			ClassAccessedEvent e = td.getClassAccessedEvent();
			e.setRRClass(c);
			getTool().classAccessed(e);
		} catch (Throwable e) {
			Assert.panic(e);
		}

	}


	public static void interruptEvent(Throwable o) {
		try {
			Util.log("Interrupted " + (o == null ? "No Exception" : "Exception") + "!!!!");
			ShadowThread td = ShadowThread.getCurrentShadowThread();
			InterruptedEvent e = td.getInterruptedEvent();
			e.setReason(o);
			getTool().interrupted(e);
			e.setReason(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}


	/*** SUPPORT FOR MULTIPLE CLASSES LOADERS ***/

	protected transient static ResourceManager<FieldInfo, ResourceManager<LoaderContext, AbstractFieldUpdater>> updatersByLoader = new ResourceManager<FieldInfo, ResourceManager<LoaderContext, AbstractFieldUpdater>>() {

		@Override
		protected ResourceManager<LoaderContext, AbstractFieldUpdater> make(final FieldInfo k) {
			return new ResourceManager<LoaderContext, AbstractFieldUpdater>() {

				@Override
				protected AbstractFieldUpdater make(LoaderContext lc) {
					final ClassInfo rrClass = k.getOwner();
					final String name = k.getName();
					final boolean isStatic = k.isStatic();
					final boolean isVolatile = k.isVolatile();
					final AbstractFieldUpdater u = lc.getGuardStateThunkObject(rrClass.getName(), name, isStatic, isVolatile);
					return u;
				}

			};
		}

	};


	protected static FieldAccessEvent prepAccessEventML(Object target, ShadowVar gs, int fadId, ShadowThread td, boolean isWrite) {
		FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(fadId);
		AbstractFieldUpdater updater;

		// must be done first!  because loading updater could trigget other accesses,
		// which will write over fields of fae.  Bitter...
		if (target != null) {
			LoaderContext lc = Loader.get(target.getClass().getClassLoader());
			updater = updatersByLoader.get(fad.getField()).get(lc);
		} else {
			updater = fad.getField().getUpdater();
		}


		FieldAccessEvent fae = td.getFieldAccessEvent();
		fae.setTarget(target);
		fae.setInfo(fad);
		fae.setUpdater(updater);
		fae.setWrite(isWrite);
		if (gs == null) {
			fae.putOriginalShadow(null);
			gs = getTool().makeShadowVar(fae);
			Assert.assertTrue(fae.getAccessInfo() == fad);
			if (!fae.putShadow(gs)) {
				gs = updater.getState(target);
				Assert.assertTrue(gs != null, "concurrent updates to new var state not resolved properly");
			}
		}

		fae.putOriginalShadow(gs);
		return fae;
	}	


	public static void readAccessML(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			FieldAccessEvent ae = prepAccessEventML(target, gs, fadId, td, false);
			firstAccess.access(ae);	
			ae.setInfo(null);
			ae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void writeAccessML(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			FieldAccessEvent ae = prepAccessEventML(target, gs, fadId, td, true);
			firstAccess.access(ae);
			ae.setInfo(null);
			ae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}


	protected static VolatileAccessEvent prepVolatileAccessEventML(Object target,
			ShadowVar gs, int fadId, ShadowThread td, boolean isWrite) {
		FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(fadId);
		// do first.  see above

		AbstractFieldUpdater updater;

		// must be done first!  because loading updater could trigget other accesses,
		// which will write over fields of fae.  Bitter...
		if (target != null) {
			LoaderContext lc = Loader.get(target.getClass().getClassLoader());
			updater = updatersByLoader.get(fad.getField()).get(lc);
		} else {
			updater = fad.getField().getUpdater();
		}

		VolatileAccessEvent fae = td.getVolatileAccessEvent();
		fae.setTarget(target);
		fae.setInfo(fad);
		fae.setUpdater(updater);
		fae.setWrite(isWrite);
		fae.setShadowVolatile(ShadowVolatile.get(target, fad.getField()));
		if (gs == null) { 
			fae.putOriginalShadow(null);
			gs = getTool().makeShadowVar(fae);
			if (!fae.putShadow(gs)) {
				gs = updater.getState(target);
				Assert.assertTrue(gs != null, "concurrent updates to new var state not resolved properly");
			}
		}

		fae.putOriginalShadow(gs);
		return fae;
	}

	public static void volatileWriteAccessML(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			VolatileAccessEvent fae = prepVolatileAccessEvent(target, gs, fadId, td, true);
			getTool().volatileAccess(fae);					
			fae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	public static void volatileReadAccessML(Object target, ShadowVar gs, int fadId, ShadowThread td) {
		try {
			VolatileAccessEvent fae = prepVolatileAccessEvent(target, gs, fadId, td, false);
			getTool().volatileAccess(fae);
			fae.setTarget(null);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}	
}



