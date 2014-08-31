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

package rr.replay;

import java.util.HashMap;
import java.util.Vector;

import acme.util.Util;

import rr.meta.FieldInfo;
import rr.state.AbstractArrayState;
import rr.state.ShadowThread;
import rr.state.ShadowVar;

public class ReplayArray extends AbstractArrayState {
	
	protected Vector<ShadowVar> elems = new Vector<ShadowVar>();
	
	public ReplayArray(int id) {
		super(id);
		elems.setSize(4);
	}
	
	@Override
	public AbstractArrayState getShadowForNextDim(ShadowThread td, Object element, int i) {
		return null;
	}

	@Override
	public ShadowVar getState(int x) {
		ensureSize(x);
		return elems.get(x);
	}

	private void ensureSize(int x) {
		while (x >= elems.size()) {
			elems.setSize(elems.size() * 2);
		}
	}

	@Override
	public boolean putState(int x, ShadowVar expected, ShadowVar v) {
		ensureSize(x);
		elems.set(x, v);
		return true;
	}

	@Override
	public void setShadowForNextDim(int i, AbstractArrayState s) {	
	}
	
}
