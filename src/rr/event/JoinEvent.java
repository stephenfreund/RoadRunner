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

import rr.meta.JoinInfo;
import rr.state.ShadowThread;

/** Represents a call to Thread.join by the target program. */
public class JoinEvent extends Event {
	
	/** The thread that we are going to join on (that is, wait for).
	 * Note joiningThread.tid could be -1 if this event happens after the tid is reused for a new thread...
	 */
	private ShadowThread joiningThread;
	
	/** Syntactic information about this operation. */
	private JoinInfo info;
	
	public JoinEvent(ShadowThread td) {
		super(td);
	}

	@Override
	public String toString() {
		return String.format("Join(%d,%d)", getThread().getTid(), getJoiningThread().getTid());
	}

	/** @RRInternal */
	public void setJoiningThread(ShadowThread joiningThread) {
		this.joiningThread = joiningThread;
	}

	public ShadowThread getJoiningThread() {
		return joiningThread;
	}

	/** @RRInternal */
	public void setInfo(JoinInfo joinInfo) {
		this.info = joinInfo;
	}

	public JoinInfo getInfo() {
		return info;
	}
	
}
