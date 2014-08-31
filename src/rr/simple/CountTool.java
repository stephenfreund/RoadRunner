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
import acme.util.count.Counter;
import acme.util.option.CommandLine;

/**
 * Count the number of events for most common cases.
 */

@Abbrev("C")
final public class CountTool extends Tool {

	private final Counter accessC, volatileAccessC, arrayAccessC, acquireC, releaseC, enterC, exitC, waitC, notifyC, 
		sleepC, joinC, startC, guardStateC;

	private static int countToolNum = 0;

	public CountTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		countToolNum++;
		String fullName = name + "(" + countToolNum + ")";
		accessC = new Counter(fullName, "Access");
		volatileAccessC = new Counter(fullName, "VolatileAccess");
		arrayAccessC = new Counter(fullName, "Array Access");
		acquireC = new Counter(fullName, "Acquire");
		releaseC = new Counter(fullName, "Release");
		enterC = new Counter(fullName, "Enter");
		exitC = new Counter(fullName, "Exit");
		waitC = new Counter(fullName, "Wait");
		notifyC = new Counter(fullName, "Notify");
		sleepC = new Counter(fullName, "Sleep");
		joinC = new Counter(fullName, "Join");
		startC = new Counter(fullName, "Start");
		guardStateC = new Counter(fullName, "Guard State");
	}

	@Override
	public void access(AccessEvent fae) {
		if (fae.getKind() == Kind.ARRAY) {
			arrayAccessC.inc();
		} else {
			accessC.inc();
		}
		super.access(fae);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) {
		volatileAccessC.inc();
		super.access(fae);
	}

	@Override
	public void acquire(AcquireEvent ae) {
		acquireC.inc();
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent re) {
		releaseC.inc();
		super.release(re);
	}

	@Override
	public void enter(MethodEvent me) {
		enterC.inc();
		super.enter(me);
	}

	@Override
	public void exit(MethodEvent me) {
		exitC.inc();
		super.exit(me);
	}

	@Override
	public  void preWait(WaitEvent we) {
		waitC.inc();
		super.preWait(we);
	}

	@Override
	public  void preNotify(NotifyEvent ne) { 
		notifyC.inc();
		super.preNotify(ne);
	}

	@Override
	public  void preSleep(SleepEvent e) { 
		sleepC.inc();
		super.preSleep(e);
	}

	@Override
	public  void preJoin(JoinEvent je) { 
		joinC.inc();
		super.preJoin(je);
	}

	@Override
	public void preStart (StartEvent se) {
		startC.inc();
		super.preStart(se);
	}
	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		guardStateC.inc();
		return super.makeShadowVar(fae);
	}

}
