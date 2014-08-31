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

package rr.simple;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;

import rr.annotations.Abbrev;
import rr.barrier.BarrierEvent;
import rr.barrier.BarrierListener;
import rr.barrier.BarrierMonitor;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassInitializedEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NewThreadEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.meta.AcquireInfo;
import rr.meta.ArrayAccessInfo;
import rr.meta.ClassInfo;
import rr.meta.FieldAccessInfo;
import rr.meta.FieldInfo;
import rr.meta.InterruptInfo;
import rr.meta.InvokeInfo;
import rr.meta.JoinInfo;
import rr.meta.MetaDataInfoVisitor;
import rr.meta.MethodInfo;
import rr.meta.ReleaseInfo;
import rr.meta.StartInfo;
import rr.meta.WaitInfo;
import rr.replay.EventEnum;
import rr.replay.ReplayBarrier;
import rr.state.ShadowThread;
import rr.tool.RR;
import rr.tool.Tool;
import acme.util.Assert;
import acme.util.Util;
import acme.util.decorations.DefaultValue;
import acme.util.identityhash.WeakIdentityHashMap;
import acme.util.option.CommandLine;

/**
 * Used to create a log to for trace replaying.  Not stable.
 */

@Abbrev("LOG")
final public class ReplayLogTool extends Tool implements MetaDataInfoVisitor, BarrierListener<ReplayBarrier> {

	protected static int count = 0;
	private class MonitoredInteger {
		final int x = count++;
		@Override
		protected void finalize() {
			try {
				synchronized (out) {
					writeEventType(EventEnum.FREE);
					out.writeInt(x);
				}
			} catch (IOException e) {
				Assert.panic(e);
			}
		}
	}

	protected IdentityHashMap<ShadowThread,Integer> threads = new IdentityHashMap<ShadowThread,Integer>();
	protected WeakIdentityHashMap<Object,MonitoredInteger> objects = new WeakIdentityHashMap<Object,MonitoredInteger>();
	protected HashMap<String,Integer> strings = new HashMap<String,Integer>();
	protected DataOutputStream out;
	protected int eventCount = 0;
	protected boolean open = true;

	public ReplayLogTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	@Override
	public void init() {
		RR.nofastPathOption.set(true);
		try {
			out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream("events.rrlog"), 8192 * 32));
			addMetaDataListener(this);

			new BarrierMonitor<ReplayBarrier>(this, new DefaultValue<Object,ReplayBarrier>() {
				public ReplayBarrier get(Object k) {
					ReplayBarrier fakeBarrier = new ReplayBarrier(object(k));
					return fakeBarrier;
				}
			});

		} catch (IOException e) {
			Assert.fail(e);
		}
	}


	@Override
	public  void fini() {
		try {
			synchronized (out) {
				writeEventType(EventEnum.QUIT);
				out.close();
				open  = false;
			}
			Util.logf("Generated %,d Events", this.eventCount);
		} catch (IOException e) {
			Assert.fail(e);
		}
	}

	// requires out
	private void writeEventType(EventEnum e) throws IOException {
		out.writeInt(e.ordinal());
		eventCount++;
	}


	// requires out
	private  int stringKey(String s) throws IOException {
		synchronized (strings) {
			Integer x = strings.get(s);
			if (x == null) {
				x = strings.size();
				writeEventType(EventEnum.STRING);
				out.writeUTF(s);
			}
			strings.put(s, x);
			return x;
		}
	}

	protected  int thread(ShadowThread s) {
		synchronized(threads) {
			Integer i = threads.get(s);
			if (i == null) {
				i = new Integer(threads.size());
				threads.put(s,i);
			}
			return i;
		}
	}

	protected int object(Object s) {
		synchronized(objects) {
			MonitoredInteger i = objects.get(s);
			if (i == null) {
				i = new MonitoredInteger();
				objects.put(s,i);
			}
			return i.x;
		}
	}


	@Override
	public void create(NewThreadEvent e) {
		ShadowThread td = e.getThread();
		int thread = thread(td);
		synchronized(out) {
			if (open) {
				try {
					writeEventType(EventEnum.CREATE);
					out.writeInt(thread);
				} catch (IOException ex) {
					Assert.fail(ex);
				}
			}
		}
		super.create(e);
	}


	@Override
	public void stop(ShadowThread td) {
		int thread = thread(td);
		synchronized(out) {
			if (open) {
				try {
					writeEventType(EventEnum.STOP);
					out.writeInt(thread);
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
		}
		super.stop(td);
	}


	@Override
	public void access(AccessEvent fae) {
		try {
			int ordinal = fae.getKind().ordinal();
			int thread = thread(fae.getThread());
			int object = object(fae.getTarget());
			synchronized(out) {
				if (open) {
					int key = stringKey(fae.getAccessInfo().getKey());
					writeEventType(EventEnum.ACCESS);
					out.writeInt(ordinal);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);
					if (fae.getKind() == AccessEvent.Kind.ARRAY) {
						out.writeInt(((ArrayAccessEvent)fae).getIndex());
					}
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.access(fae);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) {
		try {
			int ordinal = fae.getKind().ordinal();
			int thread = thread(fae.getThread());
			int object = object(fae.getTarget());
			synchronized(out) {
				if (open) {
					int key = stringKey(fae.getAccessInfo().getKey());
					writeEventType(EventEnum.ACCESS);
					out.writeInt(ordinal);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}

		super.volatileAccess(fae);
	}

	@Override
	public void acquire(AcquireEvent ae) {
		try {
			int thread = thread(ae.getThread());
			int object = object(ae.getLock().getLock());
			synchronized(out) {
				if (open) {
					int key = stringKey(ae.getInfo().getKey());
					writeEventType(EventEnum.ACQUIRE);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent ae) {
		try {
			int thread = thread(ae.getThread());
			int object = object(ae.getLock().getLock());
			synchronized(out) {
				if (open) {
					int key = stringKey(ae.getInfo().getKey());
					writeEventType(EventEnum.RELEASE);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}

		super.release(ae);
	}

	@Override
	public void enter(MethodEvent me) {
		try {
			int thread = thread(me.getThread());
			int object = object(me.getTarget());
			synchronized(out) {
				if (open) {
					int key = stringKey(me.getInfo().getKey());
					writeEventType(EventEnum.ENTER);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.enter(me);
	}

	@Override
	public void exit(MethodEvent me) {
		try {
			int thread = thread(me.getThread());
			int object = object(me.getTarget());
			synchronized(out) {
				if (open) {
					int key = stringKey(me.getInfo().getKey());
					writeEventType(EventEnum.EXIT);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.exit(me);
	}

	@Override
	public void postJoin(JoinEvent je) {
		try {
			int thread = thread(je.getThread());
			int thread2 = thread(je.getJoiningThread());
			synchronized(out) {
				if (open) {
					int key = stringKey(je.getInfo().getKey());
					writeEventType(EventEnum.POSTJOIN);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(thread2);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.postJoin(je);
	}

	@Override
	public void postNotify(NotifyEvent ne) {
		try {
			int thread = thread(ne.getThread());
			int object = object(ne.getLock().getLock());
			boolean notifyAll = ne.isNotifyAll();
			synchronized(out) {
				if (open) {
					writeEventType(EventEnum.POSTNOTIFY);
					out.writeInt(thread);
					out.writeInt(object);
					out.writeBoolean(notifyAll);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.postNotify(ne);
	}

	@Override
	public void postSleep(SleepEvent se) {
		int thread = thread(se.getThread());
		synchronized(out) {
			if (open) {
				try {
					writeEventType(EventEnum.POSTSLEEP);
					out.writeInt(thread);
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
		}
		super.postSleep(se);
	}

	@Override
	public void postStart(StartEvent se) {
		try {
			int thread = thread(se.getThread());
			int thread2 = thread(se.getNewThread());
			synchronized(out) {
				if (open) {
					writeEventType(EventEnum.POSTSTART);
					out.writeInt(thread);
					out.writeInt(thread2);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.postStart(se);
	}

	@Override
	public void postWait(WaitEvent we) {
		try {
			int thread = thread(we.getThread());
			int object = object(we.getLock().getLock());
			synchronized(out) {
				if (open) {
					int key = stringKey(we.getInfo().getKey());
					writeEventType(EventEnum.POSTWAIT);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);

				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.postWait(we);
	}

	@Override
	public void preJoin(JoinEvent je) {
		try {
			int thread = thread(je.getThread());
			int thread2 = thread(je.getJoiningThread());
			synchronized(out) {
				if (open) {
					int key = stringKey(je.getInfo().getKey());
					writeEventType(EventEnum.PREJOIN);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(thread2);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.preJoin(je);
	}

	@Override
	public void preNotify(NotifyEvent ne) {
		int thread = thread(ne.getThread());
		int object = object(ne.getLock().getLock());
		boolean notifyAll = ne.isNotifyAll();
		synchronized(out) {
			if (open) {
				try {
					writeEventType(EventEnum.PRENOTIFY);
					out.writeInt(thread);
					out.writeInt(object);
					out.writeBoolean(notifyAll);
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
		}
		super.preNotify(ne);
	}

	@Override
	public void preSleep(SleepEvent se) {
		int thread = thread(se.getThread());
		synchronized(out) {
			if (open) {
				try {
					writeEventType(EventEnum.PRESLEEP);
					out.writeInt(thread);
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
		}
		super.preSleep(se);
	}

	@Override
	public void preStart(StartEvent se) {
		try {
			int thread = thread(se.getThread());
			int thread2 = thread(se.getNewThread());
			synchronized(out) {
				if (open) {
					writeEventType(EventEnum.PRESTART);
					out.writeInt(thread);
					out.writeInt(thread2);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}

		super.preStart(se);
	}


	@Override
	public void classInitialized(ClassInitializedEvent ce) {
		try {
			int thread = thread(ce.getThread());
			synchronized(out) {
				if (open) {
					int key = stringKey(ce.getRRClass().getName());
					writeEventType(EventEnum.CLASS_INITIALIZED);
					out.writeInt(thread);
					out.writeInt(key);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}

		super.classInitialized(ce);
	}

	
	@Override
	public void preWait(WaitEvent we) {
		try {
			int thread = thread(we.getThread());
			int object = object(we.getLock().getLock());
			synchronized(out) {
				if (open) {
					int key = stringKey(we.getInfo().getKey());
					writeEventType(EventEnum.PREWAIT);
					out.writeInt(thread);
					out.writeInt(key);
					out.writeInt(object);
				}
			}
		} catch (IOException e) {
			Assert.fail(e);
		}
		super.preWait(we);
	}

	public synchronized void visit(ClassInfo x) {
		synchronized(out) {
			if (open) {
				try {
					int key = stringKey(x.getName().replace("/", "."));
					writeEventType(EventEnum.LOADCLASS);
					out.writeInt(key);
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
		}
	}

	public void visit(FieldInfo x) {
		// TODO Auto-generated method stub

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

	public void visit(InvokeInfo x) {
		// TODO Auto-generated method stub

	}

	public void postDoBarrier(BarrierEvent<ReplayBarrier> be) {
		synchronized(out) {
			if (open) {
				try {
					writeEventType(EventEnum.POSTBARRIER);
					out.writeInt(thread(be.getThread()));
					out.writeInt(object(be.getBarrier()));
					out.writeInt(be.getParties());
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
		}
	}

	public synchronized void preDoBarrier(BarrierEvent<ReplayBarrier> be) {
		synchronized(out) {
			if (open) {
				try {
					writeEventType(EventEnum.PREBARRIER);
					out.writeInt(thread(be.getThread()));
					out.writeInt(object(be.getBarrier()));
					out.writeInt(be.getParties());
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
		}
	}

	public void visit(InterruptInfo x) {
		// TODO Auto-generated method stub
		Assert.panic("Implement me");
	}


}
