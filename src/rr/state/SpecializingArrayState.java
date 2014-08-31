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
import acme.util.Util;
import acme.util.count.Counter;

public final class SpecializingArrayState extends AbstractArrayState {

	private static Counter count = new Counter("Array", "Specialize Ops");
	
	protected AbstractArrayState delegate;
	protected boolean specialized;
	
	public SpecializingArrayState(Object array) {
		super(array);
		delegate = new CoarseArrayState(array);
	}

	@Override
	public synchronized void specialize() {
		if (!specialized) {
			count.inc();
			Object target = this.getArray();
			Assert.assertTrue(target != null);
			Util.logf("Specializing array %s", Util.objectToIdentityString(target));
			ShadowVar orig = delegate.getState(0);
			delegate = new FineArrayState(target);
			for (int i = 0; i < lengthOf(target); i++) {
				delegate.putState(i, null, orig);
			}
			specialized = true;
		}
	}
	
	@Override
	public AbstractArrayState getShadowForNextDim(ShadowThread td, Object element, int i) {
		return delegate.getShadowForNextDim(td, element, i);
	}

	@Override
	public void setShadowForNextDim(int i, AbstractArrayState s) {
		delegate.setShadowForNextDim(i, s);
	}

	@Override
	public final ShadowVar getState(int index) {
		return delegate.getState(index);
	}

	@Override
	public final boolean putState(int index, ShadowVar expected, ShadowVar v) {
		return delegate.putState(index, expected, v);
	}

}
