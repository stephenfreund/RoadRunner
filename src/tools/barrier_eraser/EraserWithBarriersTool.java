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

package tools.barrier_eraser;

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
import rr.event.NewThreadEvent;
import rr.event.ReleaseEvent;
import rr.event.AccessEvent.Kind;
import rr.meta.ArrayAccessInfo;
import rr.meta.FieldInfo;
import rr.simple.LastTool;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import tools.eraser.ReadSharedTool;
import tools.eraser.ReadSharedTool.ReadShared;
import tools.util.LockSet;
import acme.util.Assert;
import acme.util.Util;
import acme.util.decorations.DefaultValue;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;

/*
 * Eraser with built-in barrier support.  
 * 
 * Note: For simplicity, this version does not synchronize accesses to 
 *       the shadow memory.  It designed to simply be a simple, 
 *       example of how to use RoadRunner features.  The missing synchronization
 *       may cause it to miss an occasional race under very rare circumstances.
 * 
 */

@Abbrev("BE")
public class EraserWithBarriersTool extends Tool implements BarrierListener<Object> {

	/* Error Message Reporters for Fields and ArrayAccesses. */
	public final ErrorMessage<FieldInfo> fieldErrors = ErrorMessages.makeFieldErrorMessage("Eraser");
	public final ErrorMessage<ArrayAccessInfo> arrayErrors = ErrorMessages.makeArrayErrorMessage("Eraser");

	static final Object DUMMY = new Object();

	public EraserWithBarriersTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		if (!(next instanceof LastTool)) {
			fieldErrors.setMax(1);
			arrayErrors.setMax(1);
		}

		new BarrierMonitor<Object>(this, new DefaultValue<Object,Object>() {
			public Object get(Object k) {
				return DUMMY;
			}
		});

	}

	/* 
	 * Special Methods that the instrumentor will replace with field accesses to 
	 * an auto-generated field in ShadowThread objects.  The return type and name 
	 * of the getter indicate the name and type of field to insert.  This is
	 * solely for performance.  You can always create your own decorations on 
	 * ShadowThread objects instead 
	 */
	protected static LockSet ts_get_lset(ShadowThread td) { Assert.fail("bad"); return null;}
	protected static void ts_set_lset(ShadowThread td, LockSet ls) { Assert.fail("bad");  }
	
	protected static int ts_get_barrierClock(ShadowThread td) { Assert.fail("bad"); return -1;}
	protected static void ts_set_barrierClock(ShadowThread td, int c) { Assert.fail("bad");  }


	protected static boolean barrierTransition(ShadowThread currentThread, BEGuardState gs) {
		int currentBarrierClock = ts_get_barrierClock(currentThread);
		if (gs.barrierClock < currentBarrierClock) {
			gs.barrierClock = currentBarrierClock;
			gs.state = currentThread;
			return true;
		} else {
			return false;
		}					
	}


	@Override
	public void access(AccessEvent fae) {
		ShadowThread currentThread = fae.getThread();
		ShadowVar currentState = fae.getOriginalShadow();
		if (currentState instanceof BEGuardState) {
			BEGuardState gs = (BEGuardState)currentState;
			ShadowVar state = gs.state;

			// Thread Local
			if (state == currentThread) {
				return;
			}			

			final LockSet currentLockSet = ts_get_lset(currentThread);
			if (fae.isWrite()) {
				if (state instanceof ShadowThread) {
					if (barrierTransition(currentThread, gs)) {
						return;
					} else {
						state = gs.state = currentLockSet;
					}
				} else if (state == ReadSharedTool.ReadShared.get()) {
					if (barrierTransition(currentThread, gs)) {
						return;
					} else {
						state = gs.state = currentLockSet;
					}	
				}
			} else {
				// READ CASE

				// ThreadLocal -> Read Shared 
				if (state instanceof ShadowThread) {
					if (barrierTransition(currentThread, gs)) {
						return;
					} else {
						state = gs.state = ReadShared.get();
						return;
					}
				} else if (state == ReadShared.get()) {
					return;			
				} 
			}
			LockSet ls = (LockSet)state;
			gs.state = ls = LockSet.intersect(ls, currentLockSet);
			if (ls.isEmpty() &&  !barrierTransition(currentThread, gs))  {
				error(fae, gs);
				return;
			} 
			super.access(fae);
		}
	}

	public static boolean readFastPath(final ShadowVar g, final ShadowThread currentThread) {
		if (g instanceof BEGuardState) {
			BEGuardState gs = (BEGuardState)g;
			final ShadowVar state = gs.state;
			if (state == currentThread) {
				return true;
			} else if (state instanceof ShadowThread) {
				if (barrierTransition(currentThread, gs)) {
					return true;
				} else {
					gs.state = ReadShared.get();
					return true;
				}
			} else if (state == ReadShared.get()) {
				return true;			
			} else {
				return (lockSetCheck(currentThread, gs, state));
			}
		} else {
			return false;
		}
	}

	private static boolean lockSetCheck(final ShadowThread currentThread,
			BEGuardState gs, final ShadowVar state) {
		LockSet ls = (LockSet)state;
		ls = LockSet.intersect(ls, ts_get_lset(currentThread));
		if (ls != state) {
			gs.state = ls;
		}
		return !ls.isEmpty() || barrierTransition(currentThread, gs);
	}

	public static boolean writeFastPath(final ShadowVar g, final ShadowThread currentThread) {
		if (g instanceof BEGuardState) {
			BEGuardState gs = (BEGuardState)g;
			final ShadowVar state = gs.state;
			if (state == currentThread) {
				return true;
			} else {
				if (state instanceof ShadowThread) {
					return setToCurrentLockSet(currentThread, gs);
				} else if (state == ReadShared.get()) {
					return setToCurrentLockSet(currentThread, gs);
				}
			} 
			return lockSetCheck(currentThread, gs, state);
		} else {
			return false;
		}
	}

	private static boolean setToCurrentLockSet(final ShadowThread currentThread,
			BEGuardState gs) {
		if (barrierTransition(currentThread, gs)) {
			return true;
		} else {
			final LockSet lset = ts_get_lset(currentThread);
			gs.state = lset;
			return !lset.isEmpty();
		}
	}

	private void error(AccessEvent fae, ShadowVar g) {
		ShadowThread currentThread = fae.getThread();
		if (fae.getKind() != Kind.ARRAY) {
			FieldInfo fd = ((FieldAccessEvent)fae).getInfo().getField();

			if (fieldErrors.stillLooking(fd)) {
				fieldErrors.error(currentThread, 
						fd, 
						"Guard State",	g,
						"Class", 		fd.getOwner(),
						"Field", 		fd.getName(),
						"Target", 		Util.objectToIdentityString(fae.getTarget()),
						"Locks", 		ts_get_lset(currentThread),
						"Stack Trace",	ShadowThread.stackDumpForErrorMessage(currentThread));
			}

			if (!fieldErrors.stillLooking(fd)) advance(fae);
		} else {
			ArrayAccessInfo fd = ((ArrayAccessEvent)fae).getInfo();

			if (arrayErrors.stillLooking(fd)) {
				arrayErrors.error(currentThread, 
						fd, 
						"Guard State", 					g, 
						"Array", 						Util.objectToIdentityString(fae.getTarget()), 
						"Index", 						fae.getTarget(),
						"Locks", 						ts_get_lset(currentThread),
						"Stack Trace",					ShadowThread.stackDumpForErrorMessage(currentThread));
			}

			if (!arrayErrors.stillLooking(fd)) advance(fae);
		}
	}

	@Override
	public void acquire(AcquireEvent ae) {
		ShadowThread currentThread = ae.getThread();
		ts_set_lset(currentThread, ts_get_lset(currentThread).add(ae.getLock()));
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent re) {
		ShadowThread currentThread = re.getThread();
		ts_set_lset(currentThread, ts_get_lset(currentThread).remove(re.getLock()));
		super.release(re);
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		if (fae.getKind() != Kind.VOLATILE) {
			return new BEGuardState(fae.getThread());
		} else {
			return new BEGuardState(fae.getThread());
		}
	}

	@Override
	public void printXML(XMLWriter xml) {
		int a[] = LockSet.cacheSizes();
		int total = 0;
		for (int i = 0; i < a.length; i++) {
			total += i * a[i];
		}		
		xml.print("lscache", total);
	}

	@Override
	public void create(NewThreadEvent e) {
		ShadowThread td = e.getThread();
		ts_set_lset(td, LockSet.emptySet());
		super.create(e);

	}


	public void postDoBarrier(BarrierEvent<Object> be) {
	}

	public void preDoBarrier(BarrierEvent<Object> be) {
		ShadowThread currentThread = be.getThread();
		ts_set_barrierClock(currentThread, ts_get_barrierClock(currentThread) + 1);
	}




}
