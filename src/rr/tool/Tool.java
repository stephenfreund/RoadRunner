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


import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Vector;

import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassAccessedEvent;
import rr.event.ClassInitializedEvent;
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
import rr.loader.Loader;
import rr.meta.MetaDataInfoVisitor;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.SingletonValue;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;

/**
 * The superclass of all analysis Tools.  To create a new tool:
 * <ul>
 *    <li> Subclass this class.
 *    <li> Override event handlers that you care about.
 *    <li> If you want an abbreviation for the command line, add an @Abbrev("...") annotation to the class declaration 
 *         (as shown in the example tools) and rerun ant. RoadRunner will then recognize that tool abbreviation.
 *    <li> (Optional) For improved performance, provide fast-path read/write methods:
 *         <pre>  
 *         		public static boolean readFastPath(ShadowVar, ShadowThread)
 *         		public static boolean writeFastPath(ShadowVar, ShadowThread)
 *         </pre>
 *         These methods are 'inlined' at each read or write in the target program.
 *         These methods return true to indicate that the event was correctly handled; if they return false, then
 *         the standard (and not inlined) 'access' method is called instead. There are
 *         corner cases where they cannot be used.  Thus, design your tool to work properly, even if
 *         they are not inserted at an access site.
 *         <p>
 *         Note: New fast path versions have been added to also have a parameter for the index of fields 
 *         within an object or within an array.  See rr.simple.SpecializedFastPathTestTool for examples.
 *         <p>
 *         Also, fast path code may assume the guard state is non-null.  On first access to a 
 *         memory location, the slow path will run with a freshly-created guard state.  Only subsequent
 *         accesses will run fastpath code.  Also, you must set the guard state to a non-null value
 *         or else the fast path code will not be called.
 *  </ul>       
 *  
 *  Event handlers are the methods of this class that take a single ...Event argument.
 *  Examples include access(), acquire(), preWait(), postWait(), etc.
 *  Each event handler is called whenever one of the threads of the target program performs the corresponding kind of operation.
 *  <p> 
 *  Certain operations of the target program (join, notify, sleep, start, and wait) invoke two event handlers: 
 *  one (such as preJoin) that is called right before that operation is performed, 
 *  and a second (such as postJoin) that is called right after that operation is performed.
 *  <p> 
 *  By default (if not overridden), event handlers simply call the event handler of the next tool in the tool chain.
 *  <p> 
 *  Every memory location has an associated 'shadow location', of type ShadowVar. 
 *  Most tools will subclass ShadowVar to store appropriate per-location information
 *  (such as the locks protecting that location, etc)
 *  <p> 
 *  Always pass events along the tool chain, unless you are actively filtering them.  To pass along,
 *  use super.X().  That idiom helps with debugging if all tools follow suit.
 */

public abstract class Tool  {

	private final String name;
	private final Tool next;
	private final Tool nextEnter, nextExit, nextAcquire, nextRelease, nextAccess;

	private boolean hasReadFPMethod;
	private boolean hasWriteFPMethod;
	
	/*
	 * @RRExperimental
	 */
	private boolean hasArrayReadFPMethod;

	/*
	 * @RRExperimental
	 */
	private boolean hasArrayWriteFPMethod;

	/*
	 * @RRExperimental
	 */
	private boolean hasFieldReadFPMethod;

	/*
	 * @RRExperimental
	 */
	private boolean hasFieldWriteFPMethod;

	/**
	 * All tools are created by the RoadRunner infrastructure from the command line, based on @Abbrev("...") annotations.
	 * 
	 * They are constructed prior to processing the items on the command line after the "-tool" option.
	 * 
	 * name -- the print name of the tool
	 * next -- the rest of the tool chain
	 * commandLine -- the command line to which you may add new options
	 */
	public Tool(String name, Tool next, CommandLine commandLine) {
		this.name = name;
		this.next = next;

		if (next != null) {
			nextEnter = next.findFirstImplementor("enter");
			nextExit = next.findFirstImplementor("exit");
			nextAcquire = next.findFirstImplementor("acquire");
			nextRelease = next.findFirstImplementor("release");
			nextAccess = next.findFirstImplementor("access");
		} else {
			nextEnter = nextExit = nextAcquire = nextRelease = nextAccess = null;
		}

		hasReadFPMethod = implementsMethod("readFastPath");
		hasWriteFPMethod = implementsMethod("writeFastPath");
		
		hasArrayReadFPMethod = implementsMethod("arrayReadFastPath");
		hasArrayWriteFPMethod = implementsMethod("arrayWriteFastPath");

		hasFieldReadFPMethod = implementsMethod("fieldReadFastPath");
		hasFieldWriteFPMethod = implementsMethod("fieldWriteFastPath");
		
		
	}

	/**
	 * Tool-specific initialization.  Called after the entire chain has been constructed but
	 * before the target generates any events.  All command line items will be processed at this point.
	 */
	public void init() { }

	/**
	 * Tool-specific shutdown.  Called when the System is shutting down but before the
	 * xml dump happens.  Be warned: worker threads could still be running
	 * and generating events while fini() runs.
	 */
	public void fini() { }


	/**
	 * Print out tool-specific log info at the end of the run.
	 */
	public void printXML(XMLWriter xml) { }


	/******************** Event Handlers ******************/

	/**
	 * This method of all tools is called for each thread creation event in the target program.
	 * Thus, thread creation events are not 'filtered' down the tool chain. 
	 * By default, this method does nothing.
	 * <b>You MUST pass this event along</b> 
	 * 
	 */
	public void create(NewThreadEvent e) { 
		next.create(e);
	}

	
	/** Called for each thread stop event in the target program. 
	 * <b>You MUST pass this event along</b> 
	 * By default, this method does nothing.
	 * */
	public void stop(ShadowThread td) { 
		next.stop(td);
	}	

	/** Called for each access event (read or write) in the target program. 
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void access(AccessEvent fae) { nextAccess.access(fae); }

	/** Called for each volatile access event (read or write) in the target program. 
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void volatileAccess(VolatileAccessEvent fae) { next.volatileAccess(fae); }

	/** Called for each method enter event in the target program. 
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void enter(MethodEvent me) { nextEnter.enter(me); }

	/** Called for each method exit event in the target program. 
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void exit(MethodEvent me) { nextExit.exit(me); }

	/** Called for each lock acquire event in the target program. 
	 * Re-entrant lock acquires are filtered out by RoadRunner, 
	 * and do not cause acquire events.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void acquire(AcquireEvent ae) { nextAcquire.acquire(ae); }

	/** Called for each lock release event in the target program. 
	 * Re-entrant lock release are filtered out by RoadRunner, 
	 * and do not cause release events.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void release(ReleaseEvent re) { nextRelease.release(re); }

	/**
	 * @RRExperimental
	 */
	public boolean testAcquire(AcquireEvent ae) { return next.testAcquire(ae); }

	/**
	 * @RRExperimental
	 */
	public boolean testRelease(ReleaseEvent re) { return next.testRelease(re); }

	/** Called right before each wait operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void preWait(WaitEvent we) { next.preWait(we); }

	/** Called right after each wait operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void postWait(WaitEvent we) { next.postWait(we); }

	/** Called right before each notify operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void preNotify(NotifyEvent ne) { next.preNotify(ne); }

	/** Called right after each notify operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void postNotify(NotifyEvent ne) { next.postNotify(ne); }

	/** Called right before each sleep operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void preSleep(SleepEvent e) { next.preSleep(e); }

	/** Called right after each sleep operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void postSleep(SleepEvent e) { next.postSleep(e); }

	/** Called right before each join operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void preJoin(JoinEvent je) { next.preJoin(je); }

	/** Called right after each join operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void postJoin(JoinEvent je) { next.postJoin(je); }

	/** Called right before each start operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void preStart(StartEvent se) { next.preStart(se); }

	/** Called right after each start operation of the target program.
	 * By default, passes the event to the next tool in the chain.
	 * */
	public void postStart(StartEvent se) { next.postStart(se); }

	/** Called right before a thread invokes interrupt.
	 * By default, passes the event to the next tool in the chain.
	 */
	public void preInterrupt(InterruptEvent me) { next.preInterrupt(me); }

	/** Called when a thread recognizes it was interrupted (because it
	 * caught an interrupted exception, or because it tests interrupted() and gets true back).
	 */
	public void interrupted(InterruptedEvent e) { next.interrupted(e); }

	
	/** Called right after the static fields of a class have been initialized.
	 * By default, passes the event to the next tool in the chain.
	 */
	public void classInitialized(ClassInitializedEvent e) {
		next.classInitialized(e);
	}

	/** Called right before any final static field of a class is read by a thread
	 * for the first time.  This is necessary to ensure a tool can add a synchronizing
	 * edges from the initialization of final fields to the first uses of those fields
	 * in other threads.
	 */
	public void classAccessed(ClassAccessedEvent e) {
		next.classAccessed(e);
	}


	/** Makes a copy of a ShadowVar. 
	 * Called by RoadRunner as part of its handling of calls to "clone()".
	 */
	public ShadowVar cloneState(ShadowVar shadowVar) {
		return next.cloneState(shadowVar);
	}

	/**
	 * A tool should should call this method when it no longer cares
	 * about the location touched by the access event. 
	 * The ShadowVar will be initialized by the next tool in the chain,
	 * and so subsequent AccessEvents passed to this tool's access method
	 * should then be dispatched to the next tool in the chain.
	 */
	protected final void advance(AccessEvent ae) {
		ShadowVar gs = next.makeShadowVar(ae);
		final boolean result = ae.putShadow(gs);
		if (!result) {
			this.access(ae);
		} else {
			ae.putOriginalShadow(gs);
		}
	}

	/**
	 * Return a fresh variable state for this tool for the
	 * location being accessed.  Will be called only once
	 * per location per tool, when the previous tool calls advance() to indicate that it is no 
	 * longer interested in owning the location 
	 */
	public ShadowVar makeShadowVar(AccessEvent ae) {
		return next.makeShadowVar(ae);
	}
	
	/** The name of this tool. Used in auto-generated help information. */
	@Override
	public String toString() {
		return name;
	}	

	/** Generates a String representation of the entire tool chain. 
	 * No need for tools to override.
	 */
	public String toChainString() {
		return toString() + " -> " + next.toChainString();
	}

	/**
	 * RoadRunner internal method.
	 * "Visitor"-like pattern for tool chains. Applies t.apply(tool) to each tool in the chain.
	 */
	public void accept(ToolVisitor t) {
		t.apply(this);
		if (next != null) {
			next.accept(t);
		}
	}


	private boolean implementsMethod(String methodName) {
		for (Class<?> c = this.getClass(); c != Tool.class; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equals(methodName)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @RRInternal
	 */
	final Tool findFirstImplementor(String methodName) {
		return implementsMethod(methodName) ? this : next.findFirstImplementor(methodName);
	}

	/**
	 * @RRInternal
	 */
	final List<Tool> findAllImplementors(final String methodName) {
		final Vector<Tool> v = new Vector<Tool>();
		this.accept(new ToolVisitor() {
			public void apply(Tool t) {
				if (t.implementsMethod(methodName)) {
					v.add(t);
				}
			}
		});
		return v;
	}

	/**
	 * @RRInternal
	 */
	public boolean hasFPMethod(boolean isWrite) {
		return isWrite ? hasWriteFPMethod : hasReadFPMethod;
	}

	/**
	 * @RRInternal
	 */
	public boolean hasArrayFPMethod(boolean isWrite) {
		return isWrite ? hasArrayWriteFPMethod : hasArrayReadFPMethod;
	}

	/**
	 * @RRInternal
	 */
	public boolean hasFieldFPMethod(boolean isWrite) {
		return isWrite ? hasFieldWriteFPMethod : hasFieldReadFPMethod;
	}

	/** Add a listener to be notified about class meta data as it is loaded by RoadRunner.
	 */
	protected final void addMetaDataListener(MetaDataInfoVisitor visitor) {
		Loader.addListener(visitor);
	}
	
	/**
	 * Create the simplest kind of decoration on ThreadStates.  Use the ShadowThread.makeDecoration method
	 * if you need more control.
	 */
	protected final <T extends Serializable> Decoration<ShadowThread, T> makeThreadDecoration(String name, T initial) {
		return ShadowThread.makeDecoration(name, DecorationFactory.Type.SINGLE, new SingletonValue<ShadowThread, T>(initial));
	}

	/**
	 * Create the simplest kind of decoration on ShadowLocks.  Use the ShadowLock.makeDecoration method
	 * if you need more control.
	 */
	protected final <T extends Serializable> Decoration<ShadowLock, T> makeLockDecoration(String name, T initial) {
		return ShadowLock.makeDecoration(name, DecorationFactory.Type.SINGLE, new SingletonValue<ShadowLock, T>(initial));
	}



	
}

