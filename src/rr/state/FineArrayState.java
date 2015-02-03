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

import acme.util.Yikes;

public final class FineArrayState extends AbstractArrayState {

	protected final ShadowVar[] shadowVar;
	protected final AbstractArrayState[] nextDimension;

	public FineArrayState(Object array) {
		super(array);
		int n = lengthOf(array);
		shadowVar = new ShadowVar[n];
		if (array.getClass().getComponentType().isArray()) {
			nextDimension = new AbstractArrayState[n];
			Object[] objArray = (Object[])array;
			for (int i = 0; i < n; i++) {
				nextDimension[i] = ArrayStateFactory.make(objArray[i], ArrayStateFactory.ArrayMode.FINE, false);
			}
		} else {
			nextDimension = null;
		}
	}


	@Override
	public AbstractArrayState getShadowForNextDim(ShadowThread td, Object element, int i) {
		if (element != nextDimension[i].getArray()) {
			Yikes.yikes("Stale array entry for next dim");
			nextDimension[i] = td.arrayStateFactory.get(element); 
		} 
		return nextDimension[i];
	}

	@Override
	public void setShadowForNextDim(int i, AbstractArrayState s) {
		nextDimension[i] = s; 
	}

	@Override
	public final ShadowVar getState(int index) {
		if (index >= shadowVar.length) {
			Yikes.yikes("Bad shadow array get: out of bounds.  Using index 0...");
			return shadowVar[0];
		}
		return shadowVar[index];
	}

	@Override
	public final boolean putState(int index, ShadowVar expected, ShadowVar v) {
		if (index >= shadowVar.length) {
			Yikes.yikes("Bad shadow array set: out of bounds.");
			return true;
		}
		if (shadowVar[index] != expected) return false;

		shadowVar[index] = v;
		return true;
	}


}
