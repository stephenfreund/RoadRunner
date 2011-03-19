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

package tools.eraser;

import rr.annotations.Abbrev;
import rr.event.AcquireEvent;
import rr.event.ReleaseEvent;
import rr.simple.LastTool;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import tools.util.LockSet;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.option.CommandLine;

@Abbrev("PL")
public final class ProtectingLockTool extends Tool {
  
	static Decoration<ShadowLock,ShadowVar> shadowLock = ShadowLock.decoratorFactory.make("Eraser:lock", DecorationFactory.Type.MULTIPLE,
			new DefaultValue<ShadowLock,ShadowVar>() { public ShadowVar get(ShadowLock ld) { return null; }});
	
	protected ShadowVar get(ShadowLock td) {
		return shadowLock.get(td);
	}

	protected void set(ShadowLock td, ShadowVar gs) {
		shadowLock.set(td, gs);
	}
	

	
	public ProtectingLockTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	@Override
	public void acquire(AcquireEvent ae) {
		if (!protectedAfterAccess(ae.getThread(), ae.getLock(), true)) {
			super.acquire(ae);
		}
	}

	@Override
	public void release(ReleaseEvent re) {
		if (!protectedAfterAccess(re.getThread(), re.getLock(), false)) {
			super.release(re);
		}
	}

	protected boolean protectedAfterAccess(ShadowThread currentThread, ShadowLock ld, boolean isAcquire) {
		ShadowVar gs = get(ld);
		
		if (gs == null) {
			set(ld, currentThread);
			return true;
		} 
		
		if (gs == currentThread) {
			return true;
		} else if (gs instanceof ShadowThread) {
			set(ld, gs = LockSetTool.ts_get_lset(currentThread));
		} 
		
		if (gs instanceof LockSet) {
			LockSet ls = (LockSet) gs;
			ls = LockSet.intersect(ls, LockSetTool.ts_get_lset(currentThread));			
			set(ld, gs = ls);
			if (gs != LockSet.emptySet()) {
				return true;
			} else {
				gs = LastTool.getLastGuardState();
			}
		}
		return false;
	}
}
