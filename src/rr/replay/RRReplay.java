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

package rr.replay;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Vector;

import rr.RRMain;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ClassInitializedEvent;
import rr.event.JoinEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.WaitEvent;
import rr.instrument.hooks.SpecialMethods;
import rr.loader.Loader;
import rr.meta.AcquireInfo;
import rr.meta.ArrayAccessInfo;
import rr.meta.ClassInfo;
import rr.meta.FieldAccessInfo;
import rr.meta.FieldInfo;
import rr.meta.InterruptInfo;
import rr.meta.InvokeInfo;
import rr.meta.JoinInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MetaDataInfoVisitor;
import rr.meta.MethodInfo;
import rr.meta.ReleaseInfo;
import rr.meta.StartInfo;
import rr.meta.WaitInfo;
import rr.state.ArrayStateFactory;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.update.AbstractFieldUpdater;
import rr.tool.RR;
import rr.tool.RREventGenerator;
import acme.util.Assert;
import acme.util.Util;

/**
 * RRExperimental.
 */
public class RRReplay implements MetaDataInfoVisitor {

	protected Vector< ShadowThread> threads = new Vector<ShadowThread>();
	protected Vector< ReplayObject> objects = new Vector<ReplayObject>();
	protected Vector< ReplayArray> arrays= new  Vector<ReplayArray>();
	protected Vector< ReplayBarrier> barriers= new Vector<ReplayBarrier>();
	protected Vector<String> strings = new Vector<String>();
	protected int eventCount;

	protected final DataInputStream in;

	public RRReplay(String eventLog) throws IOException {
		Util.log(eventLog);
		in = new DataInputStream(new BufferedInputStream(new FileInputStream(eventLog)));
		Loader.addListener(this); 

		//		Util.addToPeriodicTasks(new PeriodicTaskStmt("Replay Stats", 1000) {
		//			@Override
		//			public void run() throws Exception {
		//				Util.logf("---------------------------");
		//				Util.logf("Events           %d", eventCount);
		//				Util.logf("Threads          %d", threads.size());
		//				Util.logf("Objects          %d", objects.size());
		//				Util.logf("Arrays           %d", arrays.size());
		//				Util.logf("Barriers         %d", barriers.size());
		//				Util.logf("Strings          %d", strings.size());
		//				Util.logf("---------------------------");
		//			}
		//		});
	}

	boolean doIt = true;

	static protected <T> void put(int index, Vector<T> ts, T t) {
		while (index >= ts.size()) {
			ts.setSize(ts.size() * 2 + 1);
		}
		ts.set(index, t);
	}	

	static protected <T> T get(int index, Vector<T> ts) {
		while (index >= ts.size()) {
			ts.setSize(ts.size() * 2 + 1);
		}
		return ts.get(index);
	}


	public synchronized void go() {
		try {
			RR.startTimer();
			boolean trackArrays = ArrayStateFactory.arrayOption.get() != ArrayStateFactory.ArrayMode.NONE;
			while (true) {
				EventEnum event = EventEnum.values()[in.readInt()];
				eventCount++;
				switch (event) {
				case LOADCLASS : {
					String className = readString();
					RRMain.loader.findClass(className);
					break;
				}
				case CREATE: {
					int thread = in.readInt();
					Thread t = new Thread();
					ShadowThread ts = ShadowThread.make(t, null);
					put(thread, threads, ts);
					break;
				}
				case ACCESS: {
					AccessEvent.Kind kind = AccessEvent.Kind.values()[in.readInt()];
					int thread = in.readInt();
					String accessKey = readString();
					int target = in.readInt();
					switch(kind) {
					case VOLATILE: {
						FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(accessKey);
						Assert.assertTrue(fad != null, "Bad MetaData");
						final Object obj = object(target);
						final ShadowVar state = fad.getField().getUpdater().getState(obj);
						if (fad.isWrite()) {
							if (doIt) 	RREventGenerator.volatileWriteAccess(obj, state, fad.getId(), thread(thread));
						} else {
							if (doIt) RREventGenerator.volatileReadAccess(obj, state, fad.getId(), thread(thread));
						}
						break;
					} 
					case FIELD: {
						FieldAccessInfo fad = MetaDataInfoMaps.getFieldAccesses().get(accessKey);
						Assert.assertTrue(fad != null, "Bad MetaData");
						final Object obj = object(target);
						final ShadowVar state = fad.getField().getUpdater().getState(obj);
						if (fad.isWrite()) {
							if (doIt) {
								RREventGenerator.writeAccess(obj, state, fad.getId(), thread(thread));
							}
						} else {
							if (doIt) RREventGenerator.readAccess(obj, state, fad.getId(), thread(thread));
						}
						break;
					} 
					case ARRAY: {
						ArrayAccessInfo fad = MetaDataInfoMaps.getArrayAccesses().get(accessKey);
						int index = in.readInt();
						if (!trackArrays) {
							break;
						}
						Assert.assertTrue(fad != null, "Bad MetaData for " + accessKey);
						if (fad.isWrite()) {
							if (doIt) RREventGenerator.arrayWrite(array(target), index, fad.getId(), thread(thread), array(target));
						} else {
							if (doIt) RREventGenerator.arrayRead(array(target), index, fad.getId(), thread(thread), array(target));
						}
						break;
					}
					}
					break;
				}

				case ACQUIRE: {
					int thread = in.readInt();
					String accessKey = readString();
					AcquireInfo fad = MetaDataInfoMaps.getAcquires().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for '" + accessKey + "'");
					int obj = in.readInt();
					ReplayObject object = object(obj);
					ShadowThread td = thread(thread);
					AcquireEvent ae = td.getAcquireEvent();
					ae.setInfo(fad);
					ae.setLock(ShadowLock.get(object));
					if (doIt) RREventGenerator.getTool().acquire(ae); 
					break;
				}
				case RELEASE: {
					int thread = in.readInt();
					String accessKey = readString();
					ReleaseInfo fad = MetaDataInfoMaps.getReleases().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for '" + accessKey + "'");
					int obj = in.readInt();
					ReplayObject object = object(obj);
					ShadowThread td = thread(thread);
					ReleaseEvent ae = td.getReleaseEvent();
					ae.setInfo(fad);
					ae.setLock(ShadowLock.get(object));
					if (doIt) RREventGenerator.getTool().release(ae); 
					break;
				}
				case ENTER: {
					int thread = in.readInt();
					String accessKey = readString();
					MethodInfo fad = MetaDataInfoMaps.getMethods().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for " + accessKey);
					int obj = in.readInt();
					if (doIt) RREventGenerator.enter(object(obj), fad.getId(), thread(thread));
					break;
				}
				case EXIT: {
					int thread = in.readInt();
					String accessKey = readString();
					MethodInfo fad = MetaDataInfoMaps.getMethods().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for " + accessKey);
					int obj = in.readInt();
					if (doIt) RREventGenerator.exit(thread(thread));
					break;
				}

				case STOP: {
					int thread = in.readInt();
					thread(thread).terminate();
					break;
				}

				case PRESTART: {
					int thread = in.readInt();

					int newThread = in.readInt();

					ShadowThread td = thread(thread);
					StartEvent se = td.getStartEvent();
					se.setNewThread(thread(newThread));
					if (doIt) RREventGenerator.getTool().preStart(se);
					break;

				}
				case POSTSTART: {
					int thread = in.readInt();
					int newThread = in.readInt();

					ShadowThread td = thread(thread);
					StartEvent se = td.getStartEvent();
					se.setNewThread(thread(newThread));
					if (doIt) RREventGenerator.getTool().postStart(se);
					break;

				}

				case PRESLEEP: {
					int thread = in.readInt();
					SleepEvent sleepEvent = thread(thread).getSleepEvent();
					if (doIt) RR.getTool().preSleep(sleepEvent);
					break;
				}
				case POSTSLEEP: {
					int thread = in.readInt();
					SleepEvent sleepEvent = thread(thread).getSleepEvent();
					if (doIt) RR.getTool().postSleep(sleepEvent);
					break;
				}

				case PREBARRIER: {
					int td = in.readInt();
					int barrier = in.readInt();
					int parties = in.readInt();
					if (doIt) SpecialMethods.invoke("ReplayBarrier.await()V", true, barrier(barrier,parties), thread(td));
					break;
				}
				case POSTBARRIER: {
					int td = in.readInt();
					int barrier = in.readInt();
					int parties = in.readInt();
					if (doIt) SpecialMethods.invoke("ReplayBarrier.await()V", false, barrier(barrier,parties), thread(td));
					break;
				}


				case PREJOIN: {
					ShadowThread td = thread(in.readInt());
					String accessKey = readString();
					JoinInfo fad = MetaDataInfoMaps.getJoins().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for " + accessKey);

					ShadowThread joiningThread = thread(in.readInt());

					JoinEvent je = td.getJoinEvent();
					je.setJoiningThread(joiningThread);
					je.setInfo(fad);

					if (doIt) RREventGenerator.getTool().preJoin(je);
					break;
				}

				case POSTJOIN: {
					ShadowThread td = thread(in.readInt());
					String accessKey = readString();
					JoinInfo fad = MetaDataInfoMaps.getJoins().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for " + accessKey);

					ShadowThread joiningThread = thread(in.readInt());

					JoinEvent je = td.getJoinEvent();
					je.setJoiningThread(joiningThread);
					je.setInfo(fad);

					if (doIt) RREventGenerator.getTool().postJoin(je);
					break;
				} 

				case PRENOTIFY: {
					ShadowThread td = thread(in.readInt());
					Object o = object(in.readInt());
					boolean all = in.readBoolean();
					NotifyEvent ne = td.getNotifyEvent();

					ne.setLock(ShadowLock.get(o));
					ne.setNotifyAll(all);

					if (doIt) RREventGenerator.getTool().preNotify(ne);
					break;
				}
				case POSTNOTIFY: {
					ShadowThread td = thread(in.readInt());
					Object o = object(in.readInt());
					boolean all = in.readBoolean();
					NotifyEvent ne = td.getNotifyEvent();

					ne.setLock(ShadowLock.get(o));
					ne.setNotifyAll(all);

					if (doIt) RREventGenerator.getTool().postNotify(ne);
					break;
				}
				case PREWAIT: {
					ShadowThread td = thread(in.readInt());
					String accessKey = readString();
					WaitInfo fad = MetaDataInfoMaps.getWaits().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for " + accessKey);

					Object o = object(in.readInt());

					WaitEvent je = td.getWaitEvent();
					je.setInfo(fad);
					je.setLock(ShadowLock.get(o));
					if (doIt) RREventGenerator.getTool().preWait(je);
					break;
				}
				case POSTWAIT: {
					ShadowThread td = thread(in.readInt());
					String accessKey = readString();
					WaitInfo fad = MetaDataInfoMaps.getWaits().get(accessKey);
					Assert.assertTrue(fad != null, "Bad MetaData for " + accessKey);

					Object o = object(in.readInt());

					WaitEvent je = td.getWaitEvent();
					je.setInfo(fad);
					je.setLock(ShadowLock.get(o));
					if (doIt) RREventGenerator.getTool().postWait(je);
					break;
				}
				
				case CLASS_INITIALIZED: {
					ShadowThread td = thread(in.readInt());
					String classKey = readString();
					ClassInitializedEvent ce = td.getClassInitEvent();
					ce.setRRClass(MetaDataInfoMaps.getClass(classKey));
					if (doIt) RREventGenerator.getTool().classInitialized(ce);
					break;
				}
					

				case STRING: {
					String s = in.readUTF();
					strings.add(s);
					break;
				}

				case FREE: {
					int id = in.readInt();
					if (doIt && objects.size() > id) objects.set(id, null);
					if (doIt && arrays.size() > id) arrays.set(id, null);
					break;
				}

				
				
				case QUIT: {
					RR.endTimer();
					return;
				}
				default:	
					throw new RuntimeException("Bad Event " + event);

				}
			}  
		} catch(Throwable e) {
			Assert.panic(new Throwable("Replay Error (" + e.getClass() + ") on Event " + eventCount + ": " + e, e));
		}
	}

	private synchronized String readString() throws IOException {
		int key = in.readInt();
		return strings.get(key);
	}

	private synchronized ShadowThread thread(int thread) {
		ShadowThread ts = threads.get(thread);
		Assert.assertTrue(ts != null);
		return ts;
	}

	private synchronized ReplayObject object(final int target) {
		ReplayObject o = get(target, objects);
		if (o == null) {
			o = new ReplayObject(target);
			put(target, objects, o);
		}
		return o;
	}

	private synchronized ReplayArray array(final int target) {
		ReplayArray o = get(target, arrays);
		if (o == null) {
			o = new ReplayArray(target);
			put(target, arrays, o);
		}
		return o;
	}

	private synchronized ReplayBarrier barrier(final int target, int parties) {
		ReplayBarrier o = get(target, barriers);
		if (o == null) {
			o = new ReplayBarrier(parties);
			put(target, barriers, o);
		}
		return o;
	}

	public static void main(String args[]) {
		try {
			RRReplay r = new RRReplay(args[0]);
			r.go();
		} catch (Exception e) {
			Assert.fail(e);
		}
	}

	public void visit(ClassInfo x) {
		// TODO Auto-generated method stub

	}

	public synchronized void visit(final FieldInfo x) {
		x.setUpdater(new AbstractFieldUpdater() {
			protected FieldInfo field = x;
			@Override
			public ShadowVar getState(Object o) {
				ReplayObject ro = (ReplayObject)o;
				return ro.get(x);
			}

			@Override
			public boolean putState(Object o, ShadowVar expectedGS,
					ShadowVar newGS) {
				ReplayObject ro = (ReplayObject)o;
				ro.put(x, newGS);
				return true;
			}

		});
	}

	public void visit(MethodInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(AcquireInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(ReleaseInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(ArrayAccessInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(FieldAccessInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(JoinInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(StartInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(WaitInfo x) {
		// TODO Auto-generated method stub

	}

	public void visit(InterruptInfo x) {
		// TODO Auto-generated method stub
		Assert.panic("Implement me.");
	}

	public void visit(InvokeInfo x) {
		// TODO Auto-generated method stub
		
	}

}
