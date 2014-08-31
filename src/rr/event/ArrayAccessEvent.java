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

import rr.meta.AccessInfo;
import rr.meta.ArrayAccessInfo;
import rr.state.AbstractArrayState;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.update.AbstractArrayUpdater;
import acme.util.Util;
import acme.util.Yikes;

/** An Event representing an array access (read or write) operation of the target program. */
public class ArrayAccessEvent extends AccessEvent {

	/** The index of the array access. */
	protected int index; 
	
	/** Information about the syntactic array access operation in the target. */
	protected ArrayAccessInfo info;

	/** @RRInternal */
	protected AbstractArrayState arrayState;

	/** @RRInternal */
	protected AbstractArrayUpdater updater;

	/** Called by RoadRunner to create an ArrayAccessEvent that will be re-used for all array accesses by thread td. */
	public ArrayAccessEvent(ShadowThread td) {
		super(td);
		setUpdater(td.arrayUpdater);
	}

	@Override
	public String toString() {
		if (!oldValue.isEmpty()) {
			if (isWrite) {
				return String.format("AWr(%d,%s[%d])[%s -> %s]", getThread().getTid(), Util.objectToIdentityString(target), getIndex(), 
						oldValue, 
						newValue);
			} else {
				return String.format("ARd(%d,%s[%d])[%s]", getThread().getTid(), Util.objectToIdentityString(target), getIndex(), 
						oldValue, 
						newValue);
			}
		} else {		
			return String.format("%s(%d,%s[%d])", this.isWrite ? "AWr" : "ARd", getThread().getTid(), Util.objectToIdentityString(target), getIndex());
		}
	}

	@Override
	public final boolean putShadow(ShadowVar newGS) {
		boolean b = updater.putState(arrayState, getIndex(), this.getOriginalShadow(), newGS);
		if (!b) {
			if (this.getShadow() == newGS) return true; // optimize redundant update
			Yikes.yikes("Bad Update");
			this.originalShadow = getShadow();
			return false;
		} else {
			return true;
		}
	}

	/** Gets the state of the shadow location corresponding to the accessed array element. */
	@Override
	public final ShadowVar getShadow() {
		return updater.getState(arrayState, getIndex());
	}

	@Override
	public AccessInfo getAccessInfo() {
		return getInfo();
	}

	/** @RRInternal */
	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	/** @RRInternal */
	public void setInfo(ArrayAccessInfo arrayAccessData) {
		this.info = arrayAccessData;
	}

	public ArrayAccessInfo getInfo() {
		return info;
	}

	/** @RRInternal */
	public void setArrayState(AbstractArrayState state) {
		this.arrayState = state;
	}

	public AbstractArrayState getArrayState() {
		return this.arrayState;
	}

	/** @RRInternal */
	public void setUpdater(AbstractArrayUpdater updater) {
		this.updater = updater;
	}

	/** @RRInternal */
	public AbstractArrayUpdater getUpdater() {
		return this.updater;
	}

	/** Returns Kind.ARRAY */
	@Override
	public Kind getKind() {
		return Kind.ARRAY;
	}
}
