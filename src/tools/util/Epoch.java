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

package tools.util;

import acme.util.Assert;
import acme.util.Util;
import rr.state.ShadowThread;
import rr.tool.RR;

public final class Epoch {

	/*
	 * We use a variable number of bits for ids, based on the maxTid configured
	 * on the command line.
	 */
	public static final int TID_BITS = Integer/*epoch*/.SIZE - Integer/*epoch*/.numberOfLeadingZeros(RR.maxTidOption.get());

	static {
		Assert.assertTrue(TID_BITS > 0 && TID_BITS <= 16, 
				"Epochs can only have 1-16 bits for tids, not " + TID_BITS + " --- check 0 < maxTid <= 2^16 ");
		Util.logf("Epoch will use %d bits for tids", TID_BITS);
	}

	public static final int CLOCK_BITS = Integer/*epoch*/.SIZE - TID_BITS;
	public static final int/*epoch*/ MAX_CLOCK = ( ((int/*epoch*/)1) << CLOCK_BITS) - 1;
	public static final int MAX_TID = (1 << TID_BITS) - 1;

	public static final int/*epoch*/ ZERO = 0;
	public static final int/*epoch*/ READ_SHARED = -1;

	public static int tid(int/*epoch*/ epoch) {
		return (int)(epoch >>> CLOCK_BITS);
	}

	public static int/*epoch*/ clock(int/*epoch*/ epoch) {
		return epoch & MAX_CLOCK;
	}

	// clock should not have a tid -- it should be a raw clock value.
	public static int/*epoch*/ make(int tid, int/*epoch*/ clock) {
		return (((int/*epoch*/)tid) << CLOCK_BITS) | clock;
	}


	public static int/*epoch*/ make(ShadowThread td, int/*epoch*/ clock) {
		return make(td.getTid(), clock);
	}

	public static int/*epoch*/ tick(int/*epoch*/ epoch) {
		Assert.assertTrue(clock(epoch) <= MAX_CLOCK - 1, "Epoch clock overflow");
		return epoch + 1;
	}
	
	public static int/*epoch*/ tick(int/*epoch*/ epoch, int amount) {
		Assert.assertTrue(clock(epoch) <= MAX_CLOCK - amount, "Epoch clock overflow");
		return epoch + amount;
	}
	
	
	public static boolean leq(int/*epoch*/ e1, int/*epoch*/ e2) {
		// Assert.assertTrue(tid(e1) == tid(e2));
		return clock(e1) <= clock(e2);
	}

	public static int/*epoch*/ max(int/*epoch*/ e1, int/*epoch*/ e2) {
		// Assert.assertTrue(tid(e1) == tid(e2));
		return Epoch.make(tid(e1), Math.max(clock(e1), clock(e2)));
	}

	public static String toString(int/*epoch*/ epoch) {
		if (epoch == READ_SHARED) {
			return "SHARED";
		} else {
			return String.format("(%d:%X)", tid(epoch), clock(epoch));
		}
	}

	public static void main(String args[]) {
		{
			int/*epoch*/ e = make(3,11);
			System.out.println(toString(e));
		}
		{
			int/*epoch*/ e = make(MAX_TID-1,11);
			System.out.println(toString(e));
			System.out.println(toString(tick(e)));
		}
		{
			int/*epoch*/ e = make(MAX_TID+1,11);
			System.out.println(toString(e));
		}
	}

}
