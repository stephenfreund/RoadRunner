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

package tools.hb;

import java.util.Vector;

import rr.annotations.Abbrev;
import rr.barrier.BarrierEvent;
import rr.barrier.BarrierListener;
import rr.barrier.BarrierMonitor;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.FieldAccessEvent;
import rr.event.InterruptEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.NewThreadEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.event.AccessEvent.Kind;
import rr.meta.ArrayAccessInfo;
import rr.meta.FieldInfo;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import tools.util.CV;
import tools.util.CVPair;
import acme.util.Assert;
import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.decorations.NullDefault;
import acme.util.option.CommandLine;

/**
 * VC-based HappensBefore Race Detector.
 *
 */

@Abbrev("HB")
public final class HappensBeforeTool extends Tool implements BarrierListener<HBBarrierState> {

	/* Reporters for field/array errors */
	public final ErrorMessage<FieldInfo> errors = ErrorMessages.makeFieldErrorMessage("HappensBefore");
	public final ErrorMessage<ArrayAccessInfo> arrayErrors = ErrorMessages.makeArrayErrorMessage("HappensBefore");

	public HappensBeforeTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine); 

		/* 
		 * Create a barrier monitor that will notify this tool when barrier ops happen.
		 * The get(k) method returns the initial state to associate with each barrier
		 * when the barrier is created.
		 */
		new BarrierMonitor<HBBarrierState>(this, new DefaultValue<Object,HBBarrierState>() {
			public HBBarrierState get(Object k) {
				return new HBBarrierState(ShadowLock.get(k));
			}
		});

	}

	/* 
	 * Special methods that tells the instrumentor to create a field in ShadowThread
	 * named "hb" of type "CV".  This is for performance only -- you could do the same
	 * thing using a decoration on ThreadStates.
	 */
	static CV ts_get_cv_hb(ShadowThread ts) { Assert.panic("Bad");	return null; }
	static void ts_set_cv_hb(ShadowThread ts, CV cv) { Assert.panic("Bad");  }

	private CV get(ShadowThread td) {
		return ts_get_cv_hb(td);
	}

	/*
	 * Attach a VectorClock to each object used as a lock. 
	 */
	Decoration<ShadowLock,CV> shadowLock = ShadowLock.decoratorFactory.make("HB:lock", DecorationFactory.Type.MULTIPLE,
			new DefaultValue<ShadowLock,CV>() { public CV get(ShadowLock ld) { return new CV(1); }});

	private CV get(ShadowLock td) {
		return shadowLock.get(td);
	}



	@Override
	public void create(NewThreadEvent e) {
		ShadowThread td = e.getThread();
		CV cv = new CV(td.getTid() + 1);
		ts_set_cv_hb(td, cv);
		super.create(e);

	}

	/*
	 * increment the time of the current thread.
	 */
	public void tick(ShadowThread currentThread) {
		get(currentThread).inc(currentThread.getTid());
	}

	@Override
	public void acquire(AcquireEvent ae) {
		final ShadowThread currentThread = ae.getThread();
		final ShadowLock shadowLock = ae.getLock();

		tick(currentThread);
		synchronized(shadowLock) {
			get(currentThread).max(get(shadowLock));
		}
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent re) {
		final ShadowThread currentThread = re.getThread();
		final ShadowLock shadowLock = re.getLock();
		synchronized(shadowLock) {
			get(shadowLock).assign(get(currentThread));
		}
		tick(currentThread);
		super.release(re);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) {
		ShadowVar g = fae.getOriginalShadow();
		final ShadowThread currentThread = fae.getThread();

		if (g instanceof CVPair) {
			final ShadowVar orig = fae.getOriginalShadow();
			final ShadowThread td = fae.getThread();
			CVPair p = (CVPair)g;

			final CV cv = ts_get_cv_hb(td);
			if (fae.isWrite()) {
				p.rd.max(get(currentThread));
				tick(td); 			
			} else {
				synchronized(p.rd) {
					get(currentThread).max(p.rd);
				}
			}
		}
		super.volatileAccess(fae);

	}

	// FIXME: syncrhonize-me because we have destructive races on clock-vectors, and
	// we have no "right-mover" argument
	@Override
	public void access(AccessEvent fae) {

		ShadowVar g = fae.getOriginalShadow();
		final ShadowThread currentThread = fae.getThread();


		if (g instanceof CVPair) {
			boolean passAlong = false;
			CVPair p = (CVPair)g;
			boolean isWrite = fae.isWrite();
			CV cv = get(currentThread);
//			Util.log("p=" + p);
//			Util.log("t=" + cv);
			final int tid = currentThread.getTid();
			if (isWrite) {

				// check after prev read
				passAlong |= checkAfter(p.rd, "read", currentThread, "write", fae, true, p);
				// check after prev write
				passAlong |= checkAfter(p.wr, "write", currentThread, "write", fae, true, p);
				p.wr.set(tid, cv.get(tid));
 
			} else {

				// check after prev write
				passAlong |= checkAfter(p.wr, "write", currentThread, "read", fae, true, p);
				p.rd.set(tid, cv.get(tid));
//				p.rd.max(cv);
			}
			if (passAlong) {
				advance(fae);
			}
//			Util.log("p=" + p);
//			Util.log("t=" + cv);
		} else {
			super.access(fae);
		} 
	}


	private boolean checkAfter(CV prev, String prevOp, ShadowThread currentThread, String curOp, 
			AccessEvent fad, boolean isWrite, ShadowVar p) {

		CV cv = get(currentThread);
		if(prev.anyGt(cv)) { 
			int start=0; 
			while(true) {
				start=prev.nextGt(cv, start);
				if (start==-1) {
					break;
				} 
				Object target = fad.getTarget();
				if (fad.getKind() == Kind.ARRAY) {
					ArrayAccessEvent aae = (ArrayAccessEvent)fad;
					final ArrayAccessInfo arrayAccessInfo = aae.getInfo();
					arrayErrors.error(currentThread, arrayAccessInfo,  
							"Guard State", 	prev, 
							"Array",		Util.objectToIdentityString(target) + "[" + aae.getIndex() +"]", 
							"Locks",		currentThread.getLocksHeld(), 
							"Prev Op",		prevOp+"-by-thread-"+start,  
							"Prev Op CV",	prev, 
							"Cur Op", 		curOp,
							"Cur Op CV", 	cv,
							"Stack",		ShadowThread.stackDumpForErrorMessage(currentThread));
					start++;
					return !arrayErrors.stillLooking(arrayAccessInfo);
				} else {
					FieldInfo fd = ((FieldAccessEvent)fad).getInfo().getField();
					errors.error(currentThread, fd, 
									"Guard State", 	prev, 
									"Class",		target==null?fd.getOwner():target.getClass(), 
									"Field",		Util.objectToIdentityString(target) + "." + fd, 
									"Locks",		currentThread.getLocksHeld(), 
									"Prev Op",		prevOp+"-by-thread-"+start,  
									"Prev Op CV",	prev, 
									"Cur Op", 		curOp,
									"Cur Op CV", 	cv,
									"Stack",		ShadowThread.stackDumpForErrorMessage(currentThread));
					start++;
					return !errors.stillLooking(fd);
				}
			}
			return false;
		} else {
			return false;
		}
	}


	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		return new CVPair();
	}

	@Override
	public void preStart(final StartEvent se) {

		final ShadowThread td = se.getThread();
		final ShadowThread forked = se.getNewThread();

		ts_set_cv_hb(forked, new CV(ts_get_cv_hb(td)));

		this.tick(forked);
		this.tick(td);

		super.preStart(se);
	}

	@Override
	public void preNotify(NotifyEvent we) {
		tick(we.getThread());
		synchronized(we.getLock()) {
			get(we.getLock()).max(get(we.getThread()));
		}
		tick(we.getThread());
		super.preNotify(we);
	}

	@Override
	public void preWait(WaitEvent we) {
		tick(we.getThread());
		synchronized(we.getLock()) {
			get(we.getLock()).max(get(we.getThread()));
		}
		tick(we.getThread());
		super.preWait(we);
	}

	@Override
	public void postWait(WaitEvent we) { 
		tick(we.getThread());
		synchronized(we.getLock()) {
			get(we.getThread()).max(get(we.getLock()));
		}
		tick(we.getThread());
		super.postWait(we);
	}

	@Override
	public void postJoin(JoinEvent je) { 
		final ShadowThread currentThread = je.getThread();
		final ShadowThread joinedThread = je.getJoiningThread();

		tick(currentThread);
		get(currentThread).max(get(joinedThread));
		tick(currentThread);
		super.postJoin(je);
	}

	private final Decoration<ShadowThread, CV> cvForExit = 
		ShadowThread.makeDecoration("HB:sbarrier", DecorationFactory.Type.MULTIPLE, new NullDefault<ShadowThread, CV>());

	public void postDoBarrier(BarrierEvent<HBBarrierState> be) {
		ShadowThread currentThread = be.getThread();
		CV old = cvForExit.get(currentThread);
		be.getBarrier().reset(old);
		get(currentThread).max(old);
		this.tick(currentThread);
	}

	public void preDoBarrier(BarrierEvent<HBBarrierState> be) {
		ShadowThread td = be.getThread();
		CV entering = be.getBarrier().entering;
		entering.max(get(td));	
		cvForExit.set(td, entering);
	}
	

	@Override
	public ShadowVar cloneState(ShadowVar shadowVar) {
		return null;
	}
	
	
	protected static Decoration<ShadowThread,Vector<CV>> interruptions = 
		ShadowThread.makeDecoration("interruptions", DecorationFactory.Type.MULTIPLE,
				new DefaultValue<ShadowThread, Vector<CV>>() { public Vector<CV> get(ShadowThread ld) { return new Vector<CV>(); }} );

	@Override
	public synchronized void preInterrupt(InterruptEvent me) {
		final ShadowThread td = me.getThread();
		final ShadowThread joining = me.getInterruptedThread();

		CV cv = new CV(get(td));
		interruptions.get(joining).add(cv);
		tick(td);
	}

	@Override
	public synchronized void interrupted(InterruptedEvent e) {
		final ShadowThread current = e.getThread();
		Vector<CV> v = interruptions.get(current);

		for (CV cv : v) {
			get(current).max(cv);
		} 
		
		v.clear();
		super.interrupted(e); 
	}



}
