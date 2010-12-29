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

package rr.event;

import rr.meta.WaitInfo;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import acme.util.Util;

/** Represents a call to Object.wait() by the target program. */

public class WaitEvent extends Event {

	
	private ShadowLock lock;
	private WaitInfo info;

	public WaitEvent(ShadowThread td) {
		super(td);
	}

	@Override
	public String toString() {
		return String.format("Wait(%d,%s)", getThread().getTid(), Util.objectToIdentityString(getLock().getLock()));
	}

	/** @RRInternal */
	public void setLock(ShadowLock shadowLock) {
		this.lock = shadowLock;
	}

	/** Returns the lock being waited on. */
	public ShadowLock getLock() {
		return lock;
	}

	/** @RRInternal */
	public void setInfo(WaitInfo waitInfo) {
		this.info = waitInfo;
	}

	/** Get syntactic information about this call to Object.wait(). */
	public WaitInfo getInfo() {
		return info;
	}

}
