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
import rr.event.AccessEvent.Kind;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.FieldAccessEvent;
import rr.event.InterruptEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.WaitEvent;
import rr.meta.FieldInfo;
import rr.state.AbstractArrayState;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import acme.util.Util;
import acme.util.option.CommandLine;

/** 
 * This tests the new specialized fast path methods for fields and arrays
 */
@Abbrev("SFPTool")
final public class SpecializedFastPathTestTool extends Tool {

	@Override
	public String toString() {
		return "SFPTestTool";
	}

	public SpecializedFastPathTestTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	private class ShadowObj implements ShadowVar {
		final Object target;

		public ShadowObj(Object obj) {
			this.target = obj;
		}
		
		public String toString() {
			return Util.objectToIdentityString(target);
		}

	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		return new ShadowObj(fae.getTarget());
	}

	@Override
	public final void access(AccessEvent fae) {
		if (fae.getKind() == Kind.FIELD) {
			FieldInfo field = ((FieldAccessEvent)fae).getInfo().getField();
			Util.logf("Access %s %s.%s (%d)", fae.isWrite() ? "WRITE":"READ", fae.getOriginalShadow(), field.toString(), field.getFieldOffset());
		} else {
			Util.logf("Access %s %s[%d]", fae.isWrite() ? "WRITE":"READ", fae.getOriginalShadow(), ((ArrayAccessEvent)fae).getIndex());
		}
	}

	// Does not handle enter/exit, so that the instrumentor won't instrument method invocations.  
	//	public void enter(MethodEvent me) {}
	//	public void exit(MethodEvent me) {}	

	@Override
	public void acquire(AcquireEvent ae) {}	
	@Override
	public void release(ReleaseEvent re) {}
	@Override
	public boolean testAcquire(AcquireEvent ae) { return true; }	
	@Override
	public boolean testRelease(ReleaseEvent re) { return true; }
	@Override
	public void preWait(WaitEvent we) {}
	@Override
	public void postWait(WaitEvent we) {}
	@Override
	public void preNotify(NotifyEvent ne) {}
	@Override
	public void postNotify(NotifyEvent ne) {}
	@Override
	public void preSleep(SleepEvent e) {}
	@Override
	public void postSleep(SleepEvent e) {}	
	@Override
	public void preJoin(JoinEvent je) {}
	@Override
	public void postJoin(JoinEvent je) {}
	@Override
	public void preStart(StartEvent se) {}
	@Override
	public void postStart(StartEvent se) {}

	@Override
	public void interrupted(InterruptedEvent e) { }

	@Override
	public void preInterrupt(InterruptEvent me) { }


	public static boolean readFastPath(ShadowVar vs, ShadowThread ts) {
		Util.log("Read FP");
		return true;
	}

	public static boolean writeFastPath(ShadowVar vs, ShadowThread ts) {
		Util.log("Write FP");
		return true;
	}

	public static boolean fieldReadFastPath(int offset, ShadowVar vs, ShadowThread ts) {
		Util.logf("Read FP %s.%d", vs, offset);
		return true;
	}

	public static boolean fieldWriteFastPath(int offset, ShadowVar vs, ShadowThread ts) {
		Util.logf("Write FP %s.%d", vs, offset);
		return true;
	}

	public static boolean arrayReadFastPath(int index, ShadowVar vs, ShadowThread ts) {
		if (vs == null) return false;
		Util.logf("Read FP %s[%d]", vs, index);
		return true;
	}

	public static boolean arrayWriteFastPath(int index, ShadowVar vs, ShadowThread ts) {
		if (vs == null) return false;
		Util.logf("Write FP %s[%d]", vs, index);
		return true;
	}

}
