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

package tools.fasttrack_cas;

import rr.state.ShadowThread;

public final class Epoch {
	
	public static final int CLOCK_BITS = 24;
	public static final int MAX_CLOCK = (1 << CLOCK_BITS) - 1;
	public static final int MAX_TID = (1 << (32 - CLOCK_BITS)) - 1;
	
	public static final int/*epoch*/ ZERO = 0;
	public static final int/*epoch*/ READ_SHARED = -1;
	
	public static int tid(int epoch) {
		return epoch >>> CLOCK_BITS;
	}
	
	public static int clock(int epoch) {
		return epoch & MAX_CLOCK;
	}

	public static int make(int tid, int clock) {
		return (tid << CLOCK_BITS) + clock;
	}
	

	public static int make(ShadowThread td, int clock) {
		return make(td.getTid(), clock);
	}
	
	public static int tick(int epoch) {
		return epoch + 1;
	}
	
	public static String toString(int epoch) {
		if (epoch == READ_SHARED) {
			return "(--:--)";
		} else {
			return String.format("(%d:%X)", tid(epoch), clock(epoch));
		}
	}

}
