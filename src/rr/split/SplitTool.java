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

package rr.split;

import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.ClassInitializedEvent;
import rr.event.FieldAccessEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NewThreadEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.event.AccessEvent.Kind;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.update.AbstractArrayUpdater;
import rr.state.update.AbstractFieldUpdater;
import rr.tool.RR;
import rr.tool.Tool;
import rr.tool.ToolVisitor;
import acme.util.Assert;
import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.NullDefault;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;

/**
 * RRExperimental.  Support for Parallel Composition.  SHould work, but
 * not with fast paths enabled.
 */
public class SplitTool extends Tool {

	Decoration<ShadowThread, SplitToolUpdater> updateFirst = ShadowThread.makeDecoration("SplitTool:updater-first", DecorationFactory.Type.MULTIPLE, new NullDefault<ShadowThread,SplitToolUpdater>());
	Decoration<ShadowThread, SplitToolUpdater> updateSecond = ShadowThread.makeDecoration("SplitTool:updater-second", DecorationFactory.Type.MULTIPLE, new NullDefault<ShadowThread,SplitToolUpdater>());
	Decoration<ShadowThread, SplitToolArrayUpdater> splitArrayShadow = ShadowThread.makeDecoration("SplitTool:array_updater", DecorationFactory.Type.MULTIPLE, new NullDefault<ShadowThread,SplitToolArrayUpdater>());

	protected Tool firstNext, secondNext, next;

	public SplitTool(String name, Tool firstNext, Tool secondNext, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		try {
			this.secondNext = secondNext;
			this.firstNext = firstNext;
			this.next = next;
			Util.log("Turning off fast path code -- not supported with split tool");
			RR.nofastPathOption.set(true);
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}


	@Override
	public void postStart(StartEvent se) {
		firstNext.postStart(se);
		secondNext.postStart(se);
		super.postStart(se);
	}

	@Override
	public void preStart(StartEvent se) {
		firstNext.preStart(se);
		secondNext.preStart(se);
		super.preStart(se);
	}

	@Override
	public void printXML(XMLWriter xml) {
	}

	@Override
	public void stop(ShadowThread td) {
		firstNext.stop(td);
		secondNext.stop(td);
		super.stop(td);
	}

	@Override
	public void classInitialized(ClassInitializedEvent ce) {
		firstNext.classInitialized(ce);
		secondNext.classInitialized(ce);
		super.classInitialized(ce);
	}

	@Override
	public void fini() {
//		ToolVisitor f = new ToolVisitor() {
//			public void apply(Tool t) {
//				t.fini();
//			}
//
//		};
//		firstNext.fini();
//		secondNext.fini();
	}

	@Override
	public void create(final NewThreadEvent e) {
		ShadowThread currentThread = e.getThread();
		super.create(e);
	
		firstNext.create(e);
		secondNext.create(e);

		updateFirst.set(currentThread, new SplitToolUpdater(true));
		updateSecond.set(currentThread, new SplitToolUpdater(false));
		splitArrayShadow.set(currentThread, new SplitToolArrayUpdater());
	}

	@Override
	public String toChainString() {
		return "(" + firstNext.toChainString() + " | " + secondNext.toChainString() + ")" + " -> " + next.toChainString();
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		ShadowVar makeShadowVarFirst = firstNext.makeShadowVar(fae);
		ShadowVar makeShadowVarSecond = secondNext.makeShadowVar(fae);
		return new SplitVarState(makeShadowVarFirst, makeShadowVarSecond);
	}
		
	@Override
	public ShadowVar cloneState(ShadowVar v) {
		if (v instanceof SplitVarState) {
			SplitVarState sgs = (SplitVarState)v;
			return new SplitVarState(this.firstNext.cloneState(sgs.firstGuardState), this.secondNext.cloneState(sgs.secondGuardState));
		} else {
			return super.cloneState(v);
		}
	}

	@Override
	public void access(AccessEvent fae) {
		if (fae.getKind() == Kind.ARRAY) {
			accessHelper((ArrayAccessEvent)fae);
		} else {
			accessHelper((FieldAccessEvent)fae);
		}
		super.access(fae);
	}

	protected void accessHelper(FieldAccessEvent fae) { 
		SplitVarState sgs = (SplitVarState)fae.getOriginalShadow();
		AbstractFieldUpdater old = fae.getUpdater();
		final ShadowThread currentThread = fae.getThread();

		// set up fae for first half
		SplitToolUpdater u = updateFirst.get(currentThread); 
		fae.setUpdater(u);
		u.sgs = sgs;  		// stash the guard state where our updater can find it

		// change the originalGS to be the first half
		fae.putOriginalShadow(sgs.firstGuardState);
		firstNext.access(fae);

		// do the same, but for the second half
		u = updateSecond.get(currentThread); 
		fae.setUpdater(u);
		u.sgs = sgs;
		fae.putOriginalShadow(sgs.secondGuardState);

		secondNext.access(fae);
		fae.putOriginalShadow(sgs);
		fae.setUpdater(old);
	}

	protected void accessHelper(ArrayAccessEvent aae) {
		SplitVarState sgs = (SplitVarState)aae.getOriginalShadow();
		AbstractArrayUpdater old = aae.getUpdater();
		final ShadowThread currentThread = aae.getThread();
		SplitToolArrayUpdater u = splitArrayShadow.get(currentThread);

		// set up fae for first half
		aae.setUpdater(u);
		u.sgs = sgs;  		// stash the guard state where our updater can find it
		u.setFirstHalf(true);

		// change the originalGS to be the first half
		aae.putOriginalShadow(sgs.firstGuardState);
		firstNext.access(aae);

		// do the same, but for the second half
		u.setFirstHalf(false);

		aae.putOriginalShadow(sgs.secondGuardState);
		secondNext.access(aae);

		aae.putOriginalShadow(sgs);
		aae.setUpdater(old);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) {
		SplitVarState sgs = (SplitVarState)fae.getOriginalShadow();
		AbstractFieldUpdater old = fae.getUpdater();
		final ShadowThread currentThread = fae.getThread();

		// set up fae for first half
		SplitToolUpdater u = updateFirst.get(currentThread); 
		fae.setUpdater(u);
		u.sgs = sgs;  		// stash the guard state where our updater can find it

		// change the originalGS to be the first half
		fae.putOriginalShadow(sgs.firstGuardState);
		firstNext.volatileAccess(fae);

		// do the same, but for the second half
		u = updateSecond.get(currentThread); 
		fae.setUpdater(u);
		u.sgs = sgs;
		fae.putOriginalShadow(sgs.secondGuardState);

		secondNext.volatileAccess(fae);
		fae.putOriginalShadow(sgs);
		fae.setUpdater(old);
		
		super.volatileAccess(fae); 
	}


	@Override
	public void acquire(AcquireEvent ae) {
		firstNext.acquire(ae);
		secondNext.acquire(ae);
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent re) {
		firstNext.release(re);
		secondNext.release(re);
		super.release(re);
	}

	@Override
	public boolean testAcquire(AcquireEvent ae) {
		return firstNext.testAcquire(ae) ||
		secondNext.testAcquire(ae) ||
		super.testAcquire(ae);
	}

	@Override
	public boolean testRelease(ReleaseEvent re) {
		return firstNext.testRelease(re) ||
		secondNext.testRelease(re) ||
		super.testRelease(re);
	}



	@Override
	public void enter(MethodEvent me) {
		firstNext.enter(me);
		secondNext.enter(me);
		super.enter(me);
	}

	@Override
	public void exit(MethodEvent me) {
		firstNext.exit(me);
		secondNext.exit(me);
		super.exit(me);
	}

	@Override
	public void preWait(WaitEvent we) {
		firstNext.preWait(we);
		secondNext.preWait(we);
		super.preWait(we);
	}

	@Override
	public void postWait(WaitEvent we) { 
		firstNext.postWait(we);
		secondNext.postWait(we);
		super.postWait(we);
	}

	@Override
	public void preNotify(NotifyEvent ne) { 
		firstNext.preNotify(ne);
		secondNext.preNotify(ne);
		super.preNotify(ne);
	}

	@Override
	public void postNotify(NotifyEvent ne) { 
		firstNext.preNotify(ne);
		secondNext.preNotify(ne);
		super.postNotify(ne);
	}

	@Override
	public void preSleep(SleepEvent e) {
		firstNext.preSleep(e);
		secondNext.preSleep(e);
		super.preSleep(e);
	}

	@Override
	public void postSleep(SleepEvent e) { 
		firstNext.postSleep(e);
		secondNext.postSleep(e);
		super.postSleep(e);
	}

	@Override
	public void preJoin(JoinEvent je) { 
		firstNext.preJoin(je);
		secondNext.preJoin(je);
		super.preJoin(je);
	}

	@Override
	public void postJoin(JoinEvent je) { 
		firstNext.postJoin(je);
		secondNext.postJoin(je);
		super.postJoin(je);
	}


	@Override
	public void accept(ToolVisitor t) {
		super.accept(t);
		firstNext.accept(t);
		secondNext.accept(t);
	}


	@Override
	public boolean hasFPMethod(boolean isWrite) {
		return false;
	}


	@Override
	public void init() {
		firstNext.init();
		secondNext.init();
		super.init();
		
	}
	
	
}
