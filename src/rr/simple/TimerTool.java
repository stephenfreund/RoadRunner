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

import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.event.AccessEvent.Kind;
import rr.state.ShadowVar;
import rr.tool.Tool;
import acme.util.count.Timer;
import acme.util.option.CommandLine;

/**
 * Count the number of events for most common cases.
 */

@Abbrev("T")
final public class TimerTool extends Tool {

	private final Timer accessC, volatileAccessC, arrayAccessC, acquireC, releaseC, enterC, exitC, waitC, notifyC, 
		sleepC, joinC, startC, guardStateC;

	private static int countToolNum = 0;

	public TimerTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		countToolNum++;
		String fullName = name + "(" + countToolNum + ")";
		accessC = new Timer(fullName, "Access");
		volatileAccessC = new Timer(fullName, "VolatileAccess");
		arrayAccessC = new Timer(fullName, "Array Access");
		acquireC = new Timer(fullName, "Acquire");
		releaseC = new Timer(fullName, "Release");
		enterC = new Timer(fullName, "Enter");
		exitC = new Timer(fullName, "Exit");
		waitC = new Timer(fullName, "Wait");
		notifyC = new Timer(fullName, "Notify");
		sleepC = new Timer(fullName, "Sleep");
		joinC = new Timer(fullName, "Join");
		startC = new Timer(fullName, "Start");
		guardStateC = new Timer(fullName, "Guard State");
	}

	@Override
	public void access(AccessEvent fae) {
		if (fae.getKind() == Kind.ARRAY) {
			long x = arrayAccessC.start();
			super.access(fae);
			arrayAccessC.stop(x);
		} else {
			long x = accessC.start();
			super.access(fae);
			accessC.stop(x);
		}
		
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) {
		long x = volatileAccessC.start();
		super.access(fae);
		volatileAccessC.stop(x);
	}

	@Override
	public void acquire(AcquireEvent ae) {
		long x = acquireC.start();
		super.acquire(ae);
		acquireC.stop(x);
	}

	@Override
	public void release(ReleaseEvent re) {
		long x = releaseC.start();
		super.release(re);
		releaseC.stop(x);
	}

	@Override
	public void enter(MethodEvent me) {
		long x = enterC.start();
		super.enter(me);
		enterC.stop(x);
	}

	@Override
	public void exit(MethodEvent me) {
		long x = exitC.start();
		super.exit(me);
		exitC.stop(x);
	}

	@Override
	public  void preWait(WaitEvent we) {
		long x = waitC.start();
		super.preWait(we);
		waitC.stop(x);
	}

	@Override
	public  void preNotify(NotifyEvent ne) { 
		long x = notifyC.start();
		super.preNotify(ne);
		notifyC.stop(x);
	}

	@Override
	public  void preSleep(SleepEvent e) { 
		long x = sleepC.start();
		super.preSleep(e);
		sleepC.stop(x);
	}

	@Override
	public  void preJoin(JoinEvent je) { 
		long x = joinC.start();
		super.preJoin(je);
		joinC.stop(x);
	}

	@Override
	public void preStart (StartEvent se) {
		long x = startC.start();
		super.preStart(se);
		startC.stop(x);
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		long x = guardStateC.start();
		ShadowVar v = super.makeShadowVar(fae);
		guardStateC.stop(x);
		return v;
	}

}
