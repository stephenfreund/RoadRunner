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
public final class CASCoarseArrayState extends CASAbstractArrayState {

	protected ShadowVar state;
	protected final AbstractArrayState[] nextDimension;

    private static final long offset;
    
    static {
    	long o = 0;
		try {
			o = unsafe.objectFieldOffset(CASCoarseArrayState.class.getDeclaredField("state"));
		} catch (Exception e) {
			Assert.panic(e);
		}
		offset = o;
	}

	public CASCoarseArrayState(Object array) {
		super(array);
		int n = lengthOf(array);
		if (array.getClass().getComponentType().isArray()) {
			nextDimension = new AbstractArrayState[n];
			Object[] objArray = (Object[])array;
			for (int i = 0; i < n; i++) {
				nextDimension[i] = ArrayStateFactory.make(objArray[i], ArrayStateFactory.ArrayMode.COARSE, false);
			}
		} else {
			nextDimension = null;
		}
	}


	@Override
	public AbstractArrayState getShadowForNextDim(ShadowThread td, Object element, int i) {
		if (element != nextDimension[i].getArray()) {
//			Yikes.yikes("Stale array entry for next dim");
			nextDimension[i] = td.arrayStateFactory.get(element); 
		} 
		return nextDimension[i];
	}

	@Override
	public void setShadowForNextDim(int i, AbstractArrayState s) {
		nextDimension[i] = s; 
	}


	@Override
	public ShadowVar getState(int in) {
		return (ShadowVar) unsafe.getObjectVolatile(this,  offset);
	}

	public void setState(ShadowVar state) {
		this.state = state;
	}


	@Override
	public final boolean putState(int index, ShadowVar expected, ShadowVar v) {
	    boolean b = unsafe.compareAndSwapObject(this, offset, expected, v);
	    if (!b) Yikes.yikes("CASCoarseArrayState: atomic updated failed.");
		return b;

	}


}
