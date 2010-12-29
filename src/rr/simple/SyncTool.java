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
import rr.event.ClassInitializedEvent;
import rr.event.InterruptEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import acme.util.option.CommandLine;

/**
 * Synchronize all event handling.  This is useful when debugging tools.
 */

@Abbrev("S")
final public class SyncTool extends Tool {

	public SyncTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	@Override
	public synchronized void classInitialized(ClassInitializedEvent e) {
		super.classInitialized(e);
	}

	@Override
	public synchronized void access(AccessEvent fae) {
		super.access(fae);
	}

	@Override
	public synchronized void volatileAccess(VolatileAccessEvent fae) {
		super.volatileAccess(fae);
	}

	@Override
	public synchronized void enter(MethodEvent me) {
		super.enter(me);
	}

	@Override
	public synchronized void exit(MethodEvent me) {
		super.exit(me);
	}

	@Override
	public synchronized void acquire(AcquireEvent ae) {
		super.acquire(ae);
	}

	@Override
	public synchronized void release(ReleaseEvent re) {
		super.release(re);
	}

	@Override
	public synchronized boolean testAcquire(AcquireEvent ae) {
		return super.testAcquire(ae);
	}

	@Override
	public synchronized boolean testRelease(ReleaseEvent re) {
		return super.testRelease(re);
	}

	@Override
	public synchronized void preWait(WaitEvent we) {
		super.preWait(we);
	}

	@Override
	public synchronized void postWait(WaitEvent we) { 
		super.postWait(we);
	}

	@Override
	public synchronized void preNotify(NotifyEvent ne) { 
		super.preNotify(ne);
	}

	@Override
	public synchronized void postNotify(NotifyEvent ne) { 
		super.postNotify(ne);
	}

	@Override
	public synchronized void preSleep(SleepEvent e) { 
		super.preSleep(e);
	}

	@Override
	public synchronized void postSleep(SleepEvent e) { 
		super.postSleep(e);
	}

	@Override
	public synchronized void preJoin(JoinEvent je) { 
		super.preJoin(je);
	}

	@Override
	public synchronized void postJoin(JoinEvent je) { 
		super.postJoin(je);
	}
	@Override
	public synchronized void preStart(StartEvent se) { 
		super.preStart(se);
	}

	@Override
	public synchronized void postStart(StartEvent se) { 
		super.postStart(se);
	}

	@Override
	public synchronized void stop(ShadowThread td) {
		super.stop(td);
	}

	@Override
	public synchronized ShadowVar cloneState(ShadowVar shadowVar) {
		return super.cloneState(shadowVar);
	}

	@Override
	public synchronized void interrupted(InterruptedEvent e) {
		super.interrupted(e);
	}
	
	@Override
	public synchronized void preInterrupt(InterruptEvent me) {
		super.preInterrupt(me);
	}


}
