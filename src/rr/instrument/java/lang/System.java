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

package rr.instrument.java.lang;

import acme.util.Assert;

public class System {

	public static void arraycopy(java.lang.Object src, int srcIndex, java.lang.Object dst, int dstIndex, int length) {
		if (src == null || dst == null) {
			throw new NullPointerException();
		} else if (src instanceof Object[]) {
			_arraycopy((Object[])src, srcIndex, (Object[])dst, dstIndex, length);
		} else if (src instanceof int[]) {
			_arraycopy((int[])src, srcIndex, (int[])dst, dstIndex, length);
		} else if (src instanceof char[]) {
			_arraycopy((char[])src, srcIndex, (char[])dst, dstIndex, length);
		} else if (src instanceof byte[]) {
			_arraycopy((byte[])src, srcIndex, (byte[])dst, dstIndex, length);
		} else if (src instanceof short[]) {
			_arraycopy((short[])src, srcIndex, (short[])dst, dstIndex, length);
		} else if (src instanceof long[]) {
			_arraycopy((long[])src, srcIndex, (long[])dst, dstIndex, length);
		} else if (src instanceof boolean[]) {
			_arraycopy((boolean[])src, srcIndex, (boolean[])dst, dstIndex, length);
		} else if (src instanceof double[]) {
			_arraycopy((double[])src, srcIndex, (double[])dst, dstIndex, length);
		} else if (src instanceof float[]) {
			_arraycopy((float[])src, srcIndex, (float[])dst, dstIndex, length);
		} else {
			Assert.panic("Bad target:" + src);
		}
	}

	private static void _arraycopy(float[] src, int srcIndex, float[] dst,
			int dstIndex, int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(double[] src, int srcIndex, double[] dst,
			int dstIndex, int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(boolean[] src, int srcIndex, boolean[] dst,
			int dstIndex, int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(long[] src, int srcIndex, long[] dst, int dstIndex,
			int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(short[] src, int srcIndex, short[] dst,
			int dstIndex, int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(byte[] src, int srcIndex, byte[] dst, int dstIndex,
			int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(char[] src, int srcIndex, char[] dst, int dstIndex,
			int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(int[] src, int srcIndex, int[] dst, int dstIndex,
			int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}

	private static void _arraycopy(Object[] src, int srcIndex, Object[] dst, int dstIndex, int length) {
		for (int i = 0; i < length; i++) {
			dst[i + dstIndex] = src[i + srcIndex];
		}
	}
}
