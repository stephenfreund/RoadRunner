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

package tools.atomizer;

import rr.annotations.Abbrev;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.MethodEvent;
import rr.event.NewThreadEvent;
import rr.event.ReleaseEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.meta.AcquireInfo;
import rr.meta.MethodInfo;
import rr.meta.ReleaseInfo;
import rr.simple.MethodMonitoringTool;
import rr.state.ShadowThread;
import rr.tool.Tool;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

@Abbrev("A")
public class AtomizerTool extends MethodMonitoringTool {

	public static CommandLineOption<Boolean> noProtLocksOption = 
		CommandLine.makeBoolean("noProtLocks", false, CommandLineOption.Kind.STABLE, "no protected locks");

	public final ErrorMessage<MethodInfo> errors = 
		ErrorMessages.makeMethodErrorMessage("Atomicity");

	public AtomizerTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		commandLine.add(tools.atomizer.AtomizerTool.noProtLocksOption);

	}

	@Override
	protected String getWatchFlag() {
		return "atomics";
	}

	@Override
	protected String[] getInitialWatchCritera() {
		return new String[] { 	
				"-.*main\\(\\[Ljava/lang/String;\\)V", 
				"-.*run\\(\\)V"
		};

	}

	static Decoration<ShadowThread,AtomizerThreadState> shadowThread = ShadowThread.makeDecoration("Atomizer:shadowThread", DecorationFactory.Type.MULTIPLE,
			new DefaultValue<ShadowThread,AtomizerThreadState>() { public AtomizerThreadState get(ShadowThread td) { return new AtomizerThreadState(); }});
	
	protected AtomizerThreadState get(ShadowThread td) {
		return shadowThread.get(td);
	}
	

	@Override
	public void create(NewThreadEvent e) {
		ShadowThread td = e.getThread();
		AtomizerThreadState atd = get(td);
		atd.atomicBlocks = new AtomicBlock[100];  
		for (int i = 0; i < atd.atomicBlocks.length; i++) {
			atd.atomicBlocks[i] = new AtomicBlock(td, this.errors);
		}
		super.create(e);

	}

	@Override
	public void access(AccessEvent fae) { 
		accessHelper(fae);
		super.access(fae);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) { 
		accessHelper(fae);
		super.volatileAccess(fae);
	}

	private void accessHelper(AccessEvent fae) {
		final ShadowThread currentThread = fae.getThread();
		AtomizerThreadState atd = get(currentThread);

		for (int i = atd.count-1; i >= 0; i--) {
			atd.atomicBlocks[i].nonMover(fae.getAccessInfo());
		}
	}

		
	@Override
	public void enter(MethodEvent me) {
		if (watch(me.getInfo())) {

			final ShadowThread currentThread = me.getThread();
			final AtomizerThreadState atd = get(currentThread);
			final AtomicBlock ab = atd.atomicBlocks[atd.count++];

			ab.phase = AtomicBlock.MATCH_RIGHT;
			ab.data = me.getInfo();
			ab.commitCause = ab.errorCause = null;
		}
		super.enter(me);
	}


	@Override
	public void exit(MethodEvent me) {
		super.exit(me);
		if (watch(me.getInfo())) {
			AtomizerThreadState atd = get(me.getThread());
			--atd.count;
		}
	}

	@Override
	public void acquire(AcquireEvent ae) {
		final ShadowThread currentThread = ae.getThread();
		final AcquireInfo lad = ae.getInfo();
		final AtomizerThreadState atd = get(currentThread);

		for (int i = atd.count-1; i >= 0; i--) {
			atd.atomicBlocks[i].rightMover(lad);
		}
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent re) {
		final ShadowThread currentThread = re.getThread();
		final ReleaseInfo lrd = re.getInfo();

		final AtomizerThreadState atd = get(currentThread);

		for (int i = atd.count-1; i >= 0; i--) {
			atd.atomicBlocks[i].leftMover(lrd);
		}
		super.release(re);
	}

	@Override
	public void preWait(final WaitEvent we) {
		final AtomizerThreadState atd = get(we.getThread());

		for (int i = atd.count-1; i >= 0; i--) {
			atd.atomicBlocks[i].leftMover(we.getInfo());
		}
		super.preWait(we);
	}

	@Override
	public void postWait(final WaitEvent we) {
		final AtomizerThreadState atd = get(we.getThread());
		for (int i = atd.count-1; i >= 0; i--) {
			atd.atomicBlocks[i].rightMover(we.getInfo());
		}
		super.postWait(we);
	}
}
