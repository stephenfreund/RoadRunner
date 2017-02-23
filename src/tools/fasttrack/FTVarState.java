/******************************************************************************

Copyright (c) 2016, Cormac Flanagan (University of California, Santa Cruz)
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

package tools.fasttrack;

import rr.state.ShadowVar;
import tools.util.Epoch;
import tools.util.VectorClock;

public class FTVarState extends VectorClock implements ShadowVar {	
	// inherited values field:
	//   * if R != SHARED, then values and values[*] are protected by this.
	//   * if R == SHARED, then:
	//       - values is write-protected by this;
	//       - values[i] is write-protected by this;
	//       - values[i] is only written thread i.
	//       - values[i] is only read without the lock by thread i.
    //      Thus, once we become SHARED, only thread i updates
	//      values[i] and only thread i reads values[i] without holding
	//      the lock, so no races exist due to program order.
	
	// Write-protected by this => No concurrent writes when lock held.
	public volatile int/*epoch*/ W;
	
	// Write-protected by this => No concurrent writes when lock held.
	// if R == Epoch.SHARED, it will never change again. 
	public volatile int/*epoch*/ R;

	protected FTVarState() {
	}
	
	public FTVarState(boolean isWrite, int/*epoch*/ epoch) {
		if (isWrite) {
			R = Epoch.ZERO;
			W = epoch; 
		} else {
			W = Epoch.ZERO;
			R = epoch; 
		}		
	}

	@Override
	public synchronized void makeCV(int len) {
		super.makeCV(len);
	}

	@Override
	public synchronized String toString() {
		return String.format("[W=%s R=%s V=%s]", Epoch.toString(W), Epoch.toString(R), super.toString());
	}
}
