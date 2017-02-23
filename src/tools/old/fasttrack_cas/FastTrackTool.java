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

package tools.old.fasttrack_cas;


import rr.RRMain;
import rr.org.objectweb.asm.Opcodes;
import rr.annotations.Abbrev;
import rr.barrier.BarrierEvent;
import rr.barrier.BarrierListener;
import rr.barrier.BarrierMonitor;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassAccessedEvent;
import rr.event.ClassInitializedEvent;
import rr.event.Event;
import rr.event.FieldAccessEvent;
import rr.event.JoinEvent;
import rr.event.NewThreadEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.event.AccessEvent.Kind;
import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.meta.ArrayAccessInfo;
import rr.meta.ClassInfo;
import rr.meta.FieldInfo;
import rr.meta.MetaDataInfoMaps;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.ShadowVolatile;
import rr.tool.Tool;
import sun.misc.Unsafe;
import tools.old.util.CV;
import tools.util.Epoch;
import tools.util.EpochPair;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.decorations.NullDefault;
import acme.util.decorations.DecorationFactory.Type;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;

/**
 * A version of FastTrack that uses an optimistic synchronization strategy
 * based on atomic compare and swap operations.  It should avoid some of
 * the contention problems one may observe with the lock-based version. 
 * Note: This version has not been tested as thoroughly as the other version.
 */

public class FastTrackTool extends Tool implements BarrierListener<FastTrackBarrierState>, Opcodes {

	static final int INIT_CV_SIZE = 4;
	public final ErrorMessage<FieldInfo> fieldErrors = ErrorMessages.makeFieldErrorMessage("FastTrack");
	public final ErrorMessage<ArrayAccessInfo> arrayErrors = ErrorMessages.makeArrayErrorMessage("FastTrack");

	public static final Decoration<ClassInfo,CV> classInitTime = MetaDataInfoMaps.getClasses().makeDecoration("FastTrack:InitTime", Type.MULTIPLE, 
			new DefaultValue<ClassInfo,CV>() {
		public CV get(ClassInfo t) {
			return new CV(INIT_CV_SIZE);
		}
	});

	public FastTrackTool(final String name, final Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		new BarrierMonitor<FastTrackBarrierState>(this, new DefaultValue<Object,FastTrackBarrierState>() {
			public FastTrackBarrierState get(Object k) {
				return new FastTrackBarrierState(ShadowLock.get(k));
			}
		});
	}

	protected static int ts_get_epoch(ShadowThread ts) { Assert.panic("Bad");	return -1;	}
	protected static void ts_set_epoch(ShadowThread ts, int v) { Assert.panic("Bad");  }

	static void setEpoch(ShadowThread shadow, int v) {
		Assert.assertTrue(shadow.getTid() == Epoch.tid(v));
		ts_set_epoch(shadow,v);
	}


	protected static CV ts_get_cv(ShadowThread ts) { Assert.panic("Bad");	return null; }
	protected static void ts_set_cv(ShadowThread ts, CV cv) { Assert.panic("Bad");  }


	static final Decoration<ShadowLock,FastTrackLockData> ftLockData = ShadowLock.makeDecoration("FastTrack:ShadowLock", DecorationFactory.Type.MULTIPLE,
			new DefaultValue<ShadowLock,FastTrackLockData>() { public FastTrackLockData get(final ShadowLock ld) { return new FastTrackLockData(ld); }});

	static final FastTrackLockData get(final ShadowLock ld) {
		return ftLockData.get(ld);
	}

	static final Decoration<ShadowVolatile,FastTrackVolatileData> ftVolatileData = ShadowVolatile.makeDecoration("FastTrack:shadowVolatile", DecorationFactory.Type.MULTIPLE,
			new DefaultValue<ShadowVolatile,FastTrackVolatileData>() { public FastTrackVolatileData get(final ShadowVolatile ld) { return new FastTrackVolatileData(ld); }});

	static final FastTrackVolatileData get(final ShadowVolatile ld) {
		return ftVolatileData.get(ld);
	}


	protected ShadowVar createHelper(AccessEvent e) {
		return new FastTrackGuardState(e.isWrite(), ts_get_epoch(e.getThread()));
	}

	@Override
	final public ShadowVar makeShadowVar(final AccessEvent fae) {
		if (fae.getKind() == Kind.VOLATILE) {
			FastTrackVolatileData vd = get(((VolatileAccessEvent)fae).getShadowVolatile());
			ShadowThread currentThread = fae.getThread();
			vd.cv.max(ts_get_cv(currentThread));
			return super.makeShadowVar(fae);
		} else {
			return createHelper(fae);
		}
	}


	protected void maxAndIncEpochAndCV(ShadowThread currentThread, CV other, Event e) {
		CV cv = ts_get_cv(currentThread);
		cv.max(other);
		cv.inc(currentThread.getTid());
		setEpoch(currentThread, cv.get(currentThread.getTid()));
	}


	protected void maxEpochAndCV(ShadowThread currentThread, CV other, Event e) {
		CV cv = ts_get_cv(currentThread);
		cv.max(other);
		setEpoch(currentThread, cv.get(currentThread.getTid()));
	}


	protected void incEpochAndCV(ShadowThread currentThread, Event e) {
		CV cv = ts_get_cv(currentThread);
		cv.inc(currentThread.getTid());
		setEpoch(currentThread, cv.get(currentThread.getTid()));
	}


	@Override
	public void create(NewThreadEvent e) {
		ShadowThread currentThread = e.getThread();
		CV cv = ts_get_cv(currentThread);

		if (cv == null) {
			cv = new CV(INIT_CV_SIZE);
			ts_set_cv(currentThread, cv);
			cv.set(currentThread.getTid(), Epoch.make(currentThread.getTid(), 0));
			this.incEpochAndCV(currentThread, null);
		}

		super.create(e);

	}


	@Override
	public void stop(ShadowThread td) {
		super.stop(td);
	}

	@Override
	public void acquire(final AcquireEvent ae) {
		final ShadowThread td = ae.getThread();
		final ShadowLock shadowLock = ae.getLock();
		final FastTrackLockData fhbLockData = get(shadowLock);

		this.maxEpochAndCV(td, fhbLockData.cv, ae);

		super.acquire(ae);
	}



	@Override
	public void release(final ReleaseEvent re) {
		final ShadowThread td = re.getThread();
		final ShadowLock shadowLock = re.getLock();
		final FastTrackLockData fhbLockData = get(shadowLock);

		CV cv = ts_get_cv(td);
		fhbLockData.cv.assign(cv);
		this.incEpochAndCV(td, re);

		super.release(re);

	}

	@Override
	public void access(final AccessEvent fae) {
		final ShadowVar var = fae.getOriginalShadow();
		final ShadowThread td = fae.getThread();

		if (var instanceof FastTrackGuardState) {


			// load the error guard state set by the fast path if it exists.
			final FastTrackGuardState isItAnError = ts_get_preStateOnError(td);
			final FastTrackGuardState x = (isItAnError != null) ? isItAnError : 
				(FastTrackGuardState)var;
			ts_set_preStateOnError(td, null);

			final int tdEpoch = ts_get_epoch(td);
			final CV tdCV = ts_get_cv(td);

			Object target = fae.getTarget();
			if (target == null) {
				CV initTime = classInitTime.get(((FieldAccessEvent)fae).getInfo().getField().getOwner());
				this.maxEpochAndCV(td, initTime, fae);
			}
			if (!fae.isWrite()) {
				// READ
				retry: while (true) {
					final long orig = x.getWREpochs();
					final int lastReadEpoch = EpochPair.read(orig);

					if (lastReadEpoch == tdEpoch) {
						break;  				// commit : same epoch
					}

					final int tid = td.getTid();

					final int lastWriteEpoch = EpochPair.write(orig);

					if (lastReadEpoch == Epoch.READ_SHARED) {
						if (x.get(tid) != tdEpoch) {  // (*) racy access -> should be fine.
							synchronized(x) {
								if (orig != x.getWREpochs()) { Yikes.yikes("concurrent mod"); continue retry; }
								x.set(tid, tdEpoch);
							}
						}
					} else {
						final int lastReader = Epoch.tid(lastReadEpoch);
						if (lastReader == tid) {
							if (!x.cas(orig, lastWriteEpoch, tdEpoch)) { Yikes.yikes("concurrent mod"); continue retry; }
							// commit: read-exclusive fast case
						} else if (lastReadEpoch <= tdCV.get(lastReader)) {  
							if (!x.cas(orig, lastWriteEpoch, tdEpoch)) { Yikes.yikes("concurrent mod"); continue retry; }
							// commit: read-exclusive slow case
						} else {
							synchronized(x) {
								x.makeCV(INIT_CV_SIZE);  // do first, in case we need it at (*)
								if (!x.cas(orig, lastWriteEpoch, Epoch.READ_SHARED)) { Yikes.yikes("concurrent mod"); continue retry; }
								// commit: read share
								x.set(lastReader, lastReadEpoch); 
								x.set(td.getTid(), tdEpoch);
							}
						}
					}

					final int lastWriter = Epoch.tid(lastWriteEpoch);
					if (lastWriter != tid && lastWriteEpoch > tdCV.get(lastWriter)) {
						error(fae, 4, "write-by-thread-", lastWriter, "read-by-thread-", tid);
					}
					break;  // don't go back and retry again...
				}
			} else {
				// WRITE
				retry: while (true) {
					final long orig = x.getWREpochs();
					final int lastWriteEpoch = EpochPair.write(orig);

					if (lastWriteEpoch == tdEpoch) {
						break;	// commit: same epoch
					} 

					final int tid = td.getTid();

					final int lastReadEpoch = EpochPair.read(orig);
					if (lastReadEpoch == tdEpoch) { 
						if (!x.cas(orig, tdEpoch, tdEpoch)) { Yikes.yikes("concurrent mod"); continue retry; }
						// commit: write exclusive fast case
					} else if (lastReadEpoch != Epoch.READ_SHARED) {
						if (!x.cas(orig, tdEpoch, tdEpoch)) { Yikes.yikes("concurrent mod"); continue retry; }
						// commit: write exclusive slow case
						final int lastReader = Epoch.tid(lastReadEpoch);
						if (lastReader != tid && lastReadEpoch > tdCV.get(lastReader)) {
							error(fae, 2, "read-by-thread-", lastReader, "write-by-thread-", tid);
						}
					} else {
						synchronized(x) {
							if (!x.cas(orig, tdEpoch, tdEpoch)) { Yikes.yikes("concurrent mod"); continue retry; } 
							// commit write shared
							if (x.anyGt(tdCV)) {
								int errorCount = 0;
								for (int prevReader = x.nextGt(tdCV, 0); prevReader > -1; prevReader = x.nextGt(tdCV, prevReader + 1)) {
									if (prevReader != tid) {
										errorCount++;
										error(fae, 3, "read-by-thread-", prevReader, "write-by-thread-", tid);
									}
								}					
								if (errorCount == 0) {
									Assert.fail("Bad error count");
								}
							}
						}
					}

					final int lastWriter = Epoch.tid(lastWriteEpoch);
					if (lastWriter != tid && lastWriteEpoch > tdCV.get(lastWriter)) {
						error(fae, 1, "write-by-thread-", lastWriter, "write-by-thread-", tid);
					}
					break; // don't go back and retry
				}
			}
		} else {
			super.access(fae);
		}
	}

	@Override
	public void volatileAccess(final VolatileAccessEvent fae) {
		final ShadowVar orig = fae.getOriginalShadow();
		final ShadowThread td = fae.getThread();

		FastTrackVolatileData vd = get((fae).getShadowVolatile());
		final CV cv = ts_get_cv(td);
		if (fae.isWrite()) {
			vd.cv.max(cv);
			this.incEpochAndCV(td, fae); 		
		} else {
			this.maxEpochAndCV(td, vd.cv, fae);
		}
		super.volatileAccess(fae);
	}

	private void error(final AccessEvent ae, final int errorCase, final String prevOp, final int prevTid, final String curOp, final int curTid) {


		try {		
			if (ae instanceof FieldAccessEvent) {
				FieldAccessEvent fae = (FieldAccessEvent)ae;
				final FieldInfo fd = fae.getInfo().getField();
				final ShadowThread currentThread = fae.getThread();
				final Object target = fae.getTarget();

				if (fieldErrors.stillLooking(fd)) {
					fieldErrors.error(currentThread,
							fd,
							"Guard State", 					fae.getOriginalShadow(),
							"Current Thread",				toString(currentThread), 
							"Class",						target==null?fd.getOwner():target.getClass(),
									"Field",						Util.objectToIdentityString(target) + "." + fd, 
									"Prev Op",						prevOp + prevTid,
									"Cur Op",						curOp + curTid, 
									"Case", 						"#" + errorCase,
									"Stack",						ShadowThread.stackDumpForErrorMessage(currentThread) 
							);
				}
				if (!fieldErrors.stillLooking(fd)) {
					advance(ae);
					return;
				}
			} else {
				ArrayAccessEvent aae = (ArrayAccessEvent)ae;
				final ShadowThread currentThread = aae.getThread();
				final Object target = aae.getTarget();

				ShadowThread prevShadowThread = ShadowThread.get(prevTid);
				Thread prevThread = prevShadowThread == null ? null : prevShadowThread.getThread();

				if (arrayErrors.stillLooking(aae.getInfo())) {
					arrayErrors.error(currentThread,
							aae.getInfo(),
							"Alloc Site", 					ArrayAllocSiteTracker.get(aae.getTarget()),
							"Guard State", 					aae.getOriginalShadow(),
							"Current Thread",				toString(currentThread), 
							"Array",						Util.objectToIdentityString(target) + "[" + aae.getIndex() + "]",
							"Prev Op",						prevOp + prevTid, //+ ("name = " + prevThread == null ? prevThread.getName(): ""),
							"Cur Op",						curOp + curTid, // + ("name = " + ShadowThread.get(curTid).getThread().getName()), 
							"Case", 						"#" + errorCase,
							"Stack",						ShadowThread.stackDumpForErrorMessage(currentThread) 
							);
				}

				aae.getArrayState().specialize();

				if (!arrayErrors.stillLooking(aae.getInfo())) {
					advance(aae);
					return;
				}
			}
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	@Override
	public void preStart(final StartEvent se) {

		final ShadowThread td = se.getThread();
		final ShadowThread forked = se.getNewThread();

		final CV curCV = ts_get_cv(td);

		CV forkedCV = ts_get_cv(forked);
		this.maxAndIncEpochAndCV(forked, curCV, se);

		this.incEpochAndCV(td, se);


		super.preStart(se);
	}


	@Override
	public void postJoin(final JoinEvent je) {
		final ShadowThread td = je.getThread();
		final ShadowThread joining = je.getJoiningThread();

		// this test tells use whether the tid has been reused already or not.  Necessary
		// to still account for stopped thread, even if that thread's tid has been reused,
		// but good to know if this is happening alot...
		if (joining.getTid() != -1) {
			this.incEpochAndCV(joining, je);
			this.maxEpochAndCV(td, ts_get_cv(joining), je);
		} else {
			Yikes.yikes("Joined after tid got reused --- don't touch anything related to tid here!");
			this.maxEpochAndCV(td, ts_get_cv(joining), je);
		}

		super.postJoin(je);	
	}


	@Override
	public void preNotify(NotifyEvent we) {
		super.preNotify(we);
	}

	@Override
	public void preWait(WaitEvent we) {
		FastTrackLockData lockData = get(we.getLock());
		this.incEpochAndCV(we.getThread(), we);
		synchronized(lockData) {
			lockData.cv.max(ts_get_cv(we.getThread()));
		}
		super.preWait(we);
	}

	@Override
	public void postWait(WaitEvent we) { 
		FastTrackLockData lockData = get(we.getLock());
		this.maxEpochAndCV(we.getThread(), lockData.cv, we);
		super.postWait(we);
	}

	public static String toString(final ShadowThread td) {
		return String.format("[tid=%-2d   cv=%s   epoch=%s]", td.getTid(), ts_get_cv(td), Epoch.toString(ts_get_epoch(td)));
	}


	private final Decoration<ShadowThread, CV> cvForExit = 
			ShadowThread.makeDecoration("FT:barrier", DecorationFactory.Type.MULTIPLE, new NullDefault<ShadowThread, CV>());

	public void preDoBarrier(BarrierEvent<FastTrackBarrierState> be) {
		FastTrackBarrierState ftbe = be.getBarrier();
		ShadowThread currentThread = be.getThread();
		CV entering = ftbe.getEntering();
		entering.max(ts_get_cv(currentThread));
		cvForExit.set(currentThread, entering);
	}

	public void postDoBarrier(BarrierEvent<FastTrackBarrierState> be) {
		FastTrackBarrierState ftbe = be.getBarrier();
		ShadowThread currentThread = be.getThread();
		CV old = cvForExit.get(currentThread);
		ftbe.reset(old);
		this.maxAndIncEpochAndCV(currentThread, old, be);

	}

	@Override
	public void classInitialized(ClassInitializedEvent e) {
		final ShadowThread currentThread = e.getThread();
		final CV cv = this.ts_get_cv(currentThread);
		Util.log("Class Init for " + e + " -- " + cv);
		classInitTime.get(e.getRRClass()).max(cv);
		this.incEpochAndCV(currentThread, e);
		super.classInitialized(e);
	}

	@Override
	public void classAccessed(ClassAccessedEvent e) {
		CV initTime = classInitTime.get(e.getRRClass());
		ShadowThread currentThread = e.getThread();
		this.maxEpochAndCV(currentThread, initTime, e); 
	}

	@Override
	public void printXML(XMLWriter xml) {
		for (ShadowThread td : ShadowThread.getThreads()) {
			xml.print("thread", toString(td));
		}
	}


	/******/

	static FastTrackGuardState ts_get_preStateOnError(ShadowThread ts) { Assert.panic("Bad");	return null;	}
	static void ts_set_preStateOnError(ShadowThread ts, FastTrackGuardState v) { Assert.panic("Bad");  }

	// see notes in other version about the structure of this code.
	
	public static boolean readFastPath(final ShadowVar gs, final ShadowThread td) {
		if (gs instanceof FastTrackGuardState) {
			final FastTrackGuardState x = ((FastTrackGuardState)gs);
			final int tdEpoch = ts_get_epoch(td);
			final long orig = x.getWREpochs();
			final int lastReadEpoch = EpochPair.read(orig);

			if (lastReadEpoch == tdEpoch) {
				return true;  				// commit : same epoch
			}

			final int tid = td.getTid();

			final int lastWriteEpoch = EpochPair.write(orig);
			final CV tdCV = ts_get_cv(td);			

			if (lastReadEpoch == Epoch.READ_SHARED) {
				if (x.get(tid) != tdEpoch) {  // (*)
					synchronized(x) {
						if (orig != x.getWREpochs()) return false;
						x.set(tid, tdEpoch);
					}
				}
			} else {
				final int lastReader = Epoch.tid(lastReadEpoch);
				if (lastReader == tid) {
					if (!x.cas(orig, lastWriteEpoch, tdEpoch)) return false;
					// commit: read-exclusive fast case
				} else if (lastReadEpoch <= tdCV.get(lastReader)) {  
					if (!x.cas(orig, lastWriteEpoch, tdEpoch)) return false;
					// commit: read-exclusive slow case
				} else {
					synchronized(x) {
						x.makeCV(INIT_CV_SIZE); // do first, incase we need it above at (*)...						
						if (!x.cas(orig, lastWriteEpoch, Epoch.READ_SHARED)) return false;
						// commit: read share
						x.set(lastReader, lastReadEpoch); 
						x.set(td.getTid(), tdEpoch);
					}
				}
			}

			final int lastWriter = Epoch.tid(lastWriteEpoch);
			if (lastWriter != tid && lastWriteEpoch > tdCV.get(lastWriter)) {
				ts_set_preStateOnError(td, new FastTrackGuardState(x, orig));
				return false;
			}

			return true;

		} else {
			return false;
		}
	}

	public static boolean writeFastPath(final ShadowVar gs, final ShadowThread td) {
		if (gs instanceof FastTrackGuardState) {
			final FastTrackGuardState x = ((FastTrackGuardState)gs);
			final int tdEpoch = ts_get_epoch(td);

			final long orig = x.getWREpochs();
			final int lastWriteEpoch = EpochPair.write(orig);

			if (lastWriteEpoch == tdEpoch) {
				return true;	// commit: same epoch
			} 

			final int tid = td.getTid();
			final CV tdCV = ts_get_cv(td);

			final int lastReadEpoch = EpochPair.read(orig);
			if (lastReadEpoch == tdEpoch) { 
				if (!x.cas(orig, tdEpoch, tdEpoch)) return false;
				// commit: write exclusive fast case
			} else if (lastReadEpoch != Epoch.READ_SHARED) {
				if (!x.cas(orig, tdEpoch, tdEpoch)) return false;  // make read be tdEpoch to ignore older reads
				// commit: write exclusive slow case
				final int lastReader = Epoch.tid(lastReadEpoch);
				if (lastReader != tid && lastReadEpoch > tdCV.get(lastReader)) {
					ts_set_preStateOnError(td, new FastTrackGuardState(x, orig));
					return false;
				}
			} else {
				synchronized(x) {
					if (!x.cas(orig, tdEpoch, tdEpoch)) return false; 
					// commit write shared
					if (x.anyGt(tdCV)) {
						ts_set_preStateOnError(td, new FastTrackGuardState(x, orig));
						return false;
					}
				}
			}

			final int lastWriter = Epoch.tid(lastWriteEpoch);
			if (lastWriter != tid && lastWriteEpoch > tdCV.get(lastWriter)) {
				ts_set_preStateOnError(td, new FastTrackGuardState(x, orig));
				return false;				
			}

			return true;
		} else {
			return false;
		}
	}
}
