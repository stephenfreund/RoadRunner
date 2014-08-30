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

package rr.state;

import sun.misc.Unsafe;
import acme.util.Assert;
import acme.util.Yikes;

/*
 * This updater uses Unsafe compare and swap operations.  It assumes that volatile 
 * semantics are enforced for cas and subsequent calls to getObjectVolatile on the
 * same memory location.
 * 
 * I believe this to be true on x86, but have not tested it thoroughly.
 *   
 * Use at your own risk.
 * 
 * @RRExperimental
 */
public abstract class CASAbstractArrayState extends AbstractArrayState {

	public CASAbstractArrayState(Object array) {
		super(array);
	}

	static protected final Unsafe unsafe = Unsafe.getUnsafe();

	static private final int base; 

	protected static final int shift;

	protected static long byteOffset(int i) {
		return ((long) i << shift) + base;
	}

	static {
		base = unsafe.arrayBaseOffset(Object[].class);
		int scale = unsafe.arrayIndexScale(Object[].class);
		if ((scale & (scale - 1)) != 0)
			Assert.panic("data type scale not a power of two");
		shift = 31 - Integer.numberOfLeadingZeros(scale);
	}

	protected static final boolean cas(ShadowVar[] shadow, int index, ShadowVar expected, ShadowVar newState) {
		final boolean b = unsafe.compareAndSwapObject(shadow, byteOffset(index), expected, newState);
		if (!b) Yikes.yikes("CASAbstractArrayState: atomic updated failed: " + expected + " " + get(shadow,index) + " " + newState);
		return b;
	}

	protected static final ShadowVar get(ShadowVar[] shadow, int index) {
		return (ShadowVar) unsafe.getObjectVolatile(shadow, byteOffset(index));
	}

}
