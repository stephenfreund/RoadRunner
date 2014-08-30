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
import rr.meta.FieldAccessInfo;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.state.update.AbstractFieldUpdater;
import acme.util.Util;
import acme.util.Yikes;

/** Represents field accesses (reads or writes) performed by the target program. */

public class FieldAccessEvent extends AccessEvent {

	/** Syntactic information about this field access. */
	protected FieldAccessInfo info; 

	/** RoadRunner internal field. */
	protected AbstractFieldUpdater updater;

	public FieldAccessEvent(ShadowThread td) {
		super(td);
	}

	@Override
	public String toString() {
		return toStringHelper("");
	}

	protected String toStringHelper(String prefix) {
		if (!oldValue.isEmpty()) {
			if (isWrite) {
				return String.format("%sWr(%d,%s.%s)[%s -> %s]",
						prefix,
						getThread().getTid(), 
						Util.objectToIdentityString(target), 
						getInfo().getField().getKey(), 
						oldValue, 
						newValue);
			} else {
				return String.format("%sRd(%d,%s.%s)[%s]",  
						prefix,
						getThread().getTid(), 
						Util.objectToIdentityString(target), 
						getInfo().getField().getKey(), 
						oldValue);
			}
		} else {
			return String.format("%s%s(%d,%s.%s)", prefix, this.isWrite ? "Wr" : "Rd", getThread().getTid(), Util.objectToIdentityString(target), getInfo().getField().getKey());			
		}
	}

	@Override
	public final boolean putShadow(ShadowVar newGS) {
		boolean b = getUpdater().putState(target, this.getOriginalShadow(), newGS);
		if (!b) {
			if (this.getShadow() == newGS) return true; // optimize redundant update
			Yikes.yikes("Bad Update");
			this.originalShadow = getShadow();
			return false;
		} else {
			return true;
		}
	}

	@Override
	public final ShadowVar getShadow() {
		return getUpdater().getState(target);
	}

	@Override
	public AccessInfo getAccessInfo() {
		return getInfo();
	}

	/** @RRInternal */
	public void setInfo(FieldAccessInfo fieldAccessInfo) {
		this.info = fieldAccessInfo;
	}

	public FieldAccessInfo getInfo() {
		return info;
	}

	/** @RRInternal */
	public void setUpdater(AbstractFieldUpdater updater) {
		this.updater = updater;
	}

	/** @RRInternal */
	public AbstractFieldUpdater getUpdater() {
		return updater;
	}

	/** Returns Kind.FIELD */
	@Override
	public Kind getKind() {
		return Kind.FIELD;
	}

}
