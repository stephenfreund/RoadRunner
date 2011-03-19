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

package rr.state;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Vector;

import rr.RRMain;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassInitializedEvent;
import rr.event.FieldAccessEvent;
import rr.event.InterruptEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NewThreadEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.meta.InvokeInfo;
import rr.meta.MethodInfo;
import rr.state.update.AbstractArrayUpdater;
import rr.state.update.Updaters;
import rr.tool.RR;
import acme.util.Assert;
import acme.util.AtomicFlag;
import acme.util.StackDump;
import acme.util.Util;
import acme.util.count.Counter;
import acme.util.count.HighWaterMark;
import acme.util.decorations.Decoratable;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;

/**
 * The shadow state for each Thread object.  Tools should not access of modify
 * the contents of a ShadowThread object.  Any additional thread-specific data
 * needed by tools should be added to ShadowThread via a decoration.
 */
public class ShadowThread extends Decoratable implements ShadowVar {

	private static final DecorationFactory<ShadowThread> decorations = new DecorationFactory<ShadowThread>(); 

	/**
	 * Create a new decoration for thread states.
	 * @param <T>  Type of decoration
	 * @param name Name of decoration
	 * @param type Whether two different decorations can have the same name
	 * @param defaultValueMaker  The default value for the decoration
	 */
	public static <T> Decoration<ShadowThread, T> makeDecoration(String name, DecorationFactory.Type type, DefaultValue<ShadowThread, T> defaultValueMaker) {
		return decorations.make(name, type, defaultValueMaker);
	}

	private static ShadowThread tidMap[];

	/** 
	 * The RoadRunner thread id of the thread, or -1 if the thread has been terminated.  ReadOnly by tools.  
	 */
	protected int tid;

	/** 
	 * The Java thread object for this ShadowThread object. 
	 */
	protected final Thread thread;
	
	/**
	 * The ShadowThread for parent thread.
	 */
	protected final ShadowThread parent;
	
	/**
	 * True if the thread has stopped running.
	 * @RRInternal
	 */
	protected final AtomicFlag isStopped = new AtomicFlag();

	/*
	 * Put here to make efficient for re-entrant locks in RREventGenerator
	 */
	private final ShadowLock lockDataMap[] = new ShadowLock[100]; // FIXME?
	private int lockDataCount = 0;

	/*
	 * Put here so the instrumenter can avoid flow
	 * analysis when figuring out when we exit a block.
	 * Basically we keep the call stack here.
	 */
	private final MethodEvent blockStack[] = new MethodEvent[10000];  // ised to be 10000
	private int blockCount = 0;

	/*** Creation ***/

	private static final Counter threadDataCounter = new Counter("ShadowThread", "Count");
	private static final HighWaterMark maxCounter = new HighWaterMark("ShadowThread", "Max Live");

	/**
	 * @RRInternal
	 */
	public final AbstractArrayUpdater arrayUpdater = createArrayUpdater();
	public final ArrayStateFactory arrayStateFactory = new ArrayStateFactory();

	/**
	 * @RRInternal
	 * Never store references to these event objects in tools, since they are reused. 
	 */
	private final FieldAccessEvent 			fieldAccessEvent 		= new FieldAccessEvent(this); 
	private final VolatileAccessEvent 		volatileAccessEvent 	= new VolatileAccessEvent(this); 
	private final ArrayAccessEvent 			arrayAccessEvent 		= new ArrayAccessEvent(this); 
	private final AcquireEvent     			acquireEvent     		= new AcquireEvent(this);
	private final ReleaseEvent     			releaseEvent     		= new ReleaseEvent(this);
	private final StartEvent					startEvent				= new StartEvent(this);
	private final WaitEvent 					waitEvent				= new WaitEvent(this);
	private final JoinEvent					joinEvent				= new JoinEvent(this);
	private final InterruptEvent				interruptEvent			= new InterruptEvent(this);
	private final NotifyEvent				notifyEvent				= new NotifyEvent(this);
	private final SleepEvent					sleepEvent				= new SleepEvent(this);
	private final ClassInitializedEvent		classInitEvent			= new ClassInitializedEvent(this);
	private final InterruptedEvent		interruptedEvent			= new InterruptedEvent(this);

	/**
	 * @RRInternal
	 */
	public int invokeId = InvokeInfo.NULL.getId();


	private static void initFreeList() {
		if (tidMap == null) {
			final Integer n = rr.tool.RR.maxTidOption.get();
			tidMap = new ShadowThread[n];
			Util.log("Creating Free List With " + n + " Tids");
		}
	}

	// require ShadowThread.class
	private int allocTid(ShadowThread newThread) {
		for (int i = 0; i < tidMap.length; i++) {
			if (tidMap[i] == null) {
				tidMap[i] = newThread;
				maxCounter.set(i);
				return i;
			}
		}
		return -1;
	}

	/**
	 * Create a new ShadowThread for the thread, given the parent thread. 
	 */
	protected ShadowThread(Thread thread, ShadowThread parent) {
		Assert.assertTrue(thread != null, "Null Thread!");
		synchronized(ShadowThread.class) {
			initFreeList();
			int tid = allocTid(this);
			if (tid == -1) {
				Assert.panic("Out of Tids.  Set -maxTid to be bigger than " + rr.tool.RR.maxTidOption.get());
			}
			Util.logf("New Thread %s with tid=%d.", thread.getName(), tid);
			this.tid = tid;
			this.parent = parent;
			this.thread = thread;
		}
		for (int i = 0; i < blockStack.length; i++) {
			blockStack[i] = new MethodEvent(this);
		}
		threadDataCounter.inc();
	}

	private AbstractArrayUpdater createArrayUpdater() {
		try {
			return Updaters.arrayUpdaterClass().newInstance();
		} catch (Exception e) {
			Assert.panic(e);
		}
		return null;
	}

	@Override
	public String toString() {
		return getThread() + "[tid = " + getTid() + "]";
	}


	/**
	 * Get the ShadowLock for an object being used as a lock, or null if
	 * that object is not locked by this thread.
	 */
	protected final ShadowLock get(Object lock) {
		for (int i = lockDataCount - 1; i >= 0; i--) {
			ShadowLock ld = lockDataMap[i];
			if (ld.getLock() == lock) {
				return ld;
			}
		}
		return null;
	}

	/**
	 * Called when a lock is acquired.  Return null if already held.
	 */
	public final ShadowLock acquire(Object lock) {
		ShadowLock ld = get(lock);
		if (ld == null) {
			ld = ShadowLock.get(lock);
		}
		if (ld.inc(this) == 1) {
			lockDataMap[lockDataCount++] = ld;
			return ld;
		}
		return null;
	}

	/**
	 * Called when a lock is released.  Return null if still held.
	 */
	public final ShadowLock release(Object lock) {
		ShadowLock ld = get(lock);
		if (ld.dec(this) == 0) {
			lockDataCount--;
			return ld;
		}
		return null;
	}


	/**
	 * return the ShadowLocks for all held locks.  Only call from within the corresponding thread.
	 */
	public final Collection<ShadowLock> getLocksHeld() {
		Assert.assertTrue(Thread.currentThread() == this.getThread());
		Vector<ShadowLock> locks = new Vector<ShadowLock>();
		int n = lockDataCount;
		for (int i = 0; i < n; i++) {
			locks.add(lockDataMap[i]);
		}
		return locks;
	}

	/*
	 * @RRInternal.  Handles method stack. 
	 */
	public final MethodEvent enter(final Object target, final MethodInfo methodData) {
		blockStack[blockCount].setTarget(target);
		blockStack[blockCount].setInfo(methodData);
		blockStack[blockCount].setEnter(true);
		return blockStack[blockCount++];
	}

	/*
	 * @RRInternal.  Handles method stack. 
	 */
	public final void exit() {
		--blockCount;
	}

	/*
	 * @RRInternal.  Handles method stack. 
	 */
	public final int getBlockDepth() {
		return blockCount;
	}

	/*
	 * @RRInternal.  Handles method stack. 
	 */
	public MethodEvent getBlock(int i) {
		return blockStack[i];
	}


	/**
	 * Return the number of threads being tracked.
	 */
	public static int numThreads() {
		return threadToThreadDataMap.size();
	}

	/**
	 * Return ThreadStates for all known threads.
	 */
	public static Collection<ShadowThread> getThreads() {
		return threadToThreadDataMap.values();
	}


	public int getTid() {
		return tid;
	}

	public Thread getThread() {
		return thread;
	}

	public ShadowThread getParent() {
		return parent;
	}

	public AtomicFlag getIsStopped() {
		return isStopped;
	}

	/////////////////


	//	Map of Thread --> its ShadowThread for use in joining... and to bridge start/init
	private static Map<Thread,ShadowThread> threadToThreadDataMap = Collections.synchronizedMap(new IdentityHashMap<Thread,ShadowThread>());

	/**
	 * Create a new ShadowThread for the thread, given the parent thread. 
	 */
	public static synchronized ShadowThread make(Thread thread, ShadowThread parent) {
		final ShadowThread td = RR.noEventReuseOption.get() ? new ThreadStateNoEventReuse(thread, parent) : new ShadowThread(thread, parent); 
		threadToThreadDataMap.put(thread, td);
		final NewThreadEvent e = new NewThreadEvent(td);
		RR.getTool().create(e);
		return td;
	}

	private static ThreadLocal<ShadowThread> shadowThread = 
		new ThreadLocal<ShadowThread>() {
		@Override
		protected  
		ShadowThread initialValue() {
			synchronized (ShadowThread.class) {
				Thread thread = Thread.currentThread();
				ShadowThread td = threadToThreadDataMap.get(thread);
				if (td == null) { // Main thread case (no start() instrumented for it)
					td = make(thread, null);
				}
				return td;
			}
		}
	};

	private static Counter tdCount = new Counter("ShadowThread", "getCurrentThread() calls");

	/**
	 * Get the ShadowThread for the currently running Thread. 
	 */
	public static ShadowThread getCurrentShadowThread() {
		if (RRMain.slowMode()) tdCount.inc();
		ShadowThread td = shadowThread.get();
		return td;
	}

	/**
	 * Get the ShadowThread for the given Thread. 
	 */
	public static ShadowThread getThreadState(Thread t) {
		ShadowThread td = threadToThreadDataMap.get(t);
		if (td == null) {
			return null;
		}
		if (td.tid != -1 && tidMap[td.tid] != null &&  tidMap[td.tid] != td) {
			td.tid = -1;
		}
		return td;
	}

	/**
	 * Get the ShadowThread for the given tid.  tid must be valid. 
	 */
	public static ShadowThread get(int tid) {
		return tidMap[tid];
	}
	
	
	/**
	 * Return a String representing the call stack for a thread in the target program. 
	 */
	public static String stackDumpForErrorMessage(ShadowThread currentThread) {
		if (RR.stackOption.get()) {	
			return StackDump.stackDump(currentThread.getThread(), RR.toolCode);
		} else {
			return "Use -stacks to show stacks...";
		}
	}

	/**
	 * Indicate that a thread has stopped running.
	 */
	public void terminate() {
		synchronized (ShadowThread.class) {
			getIsStopped().setToTrue();
			Util.message("Stopping"); 
			if (!RR.noTidGCOption.get()) {
				tidMap[this.tid] = null;
			}
		}
	}

	public FieldAccessEvent getFieldAccessEvent() {
		return fieldAccessEvent;
	}

	public VolatileAccessEvent getVolatileAccessEvent() {
		return volatileAccessEvent;
	}

	public ArrayAccessEvent getArrayAccessEvent() {
		return arrayAccessEvent;
	}

	public AcquireEvent getAcquireEvent() {
		return acquireEvent;
	}

	public ReleaseEvent getReleaseEvent() {
		return releaseEvent;
	}

	public StartEvent getStartEvent() {
		return startEvent;
	}

	public WaitEvent getWaitEvent() {
		return waitEvent;
	}

	public JoinEvent getJoinEvent() {
		return joinEvent;
	}

	public InterruptEvent getInterruptEvent() {
		return interruptEvent;
	}

	public NotifyEvent getNotifyEvent() {
		return notifyEvent;
	}

	public SleepEvent getSleepEvent() {
		return sleepEvent;
	}

	public ClassInitializedEvent getClassInitEvent() {
		return classInitEvent;
	}
	
	public InterruptedEvent getInterruptedEvent() {
		return interruptedEvent;
	}
}
