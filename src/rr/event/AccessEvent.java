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
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.TaggedValue;

/** Represents an access (read or write) event by the target program. */

public abstract class AccessEvent extends Event {

	/** What kind of access: 
	 * FIELD: to a (static or non-static) non-volatile field; 
	 * ARRAY: to an array element; 
	 * or VOLATILE to a (static or non-static) volatile field.
	 */
	public static enum Kind { FIELD, ARRAY, VOLATILE, SPECIAL }

	/** For arrays and non-static (volatile or non-volatile) field accesses, indicates the array or object being accessed. 
	 * Is null for static field accesses.
	 */
	protected Object target;

	/** Whether this access is a read or a write. */
	protected boolean isWrite;

	/** The ShadowVar (aka shadow location) for the accessed location, right before the access is performed. */ 
	protected ShadowVar originalShadow;

	/** @RRExperimental
	 * When RoadRunner is configured with the -value flag, 
	 * oldValue contains the value from the accessed location. */
	public TaggedValue oldValue = new TaggedValue();

	/** @RRExperimental
	 * When RoadRunner is configured with the -value flag, 
	 * tools can update newValue and thus control the value returned to the target program for read events. */
	public TaggedValue newValue = new TaggedValue();

	/** Creates an AccessEvent for accesses by thread td. 
	 * This event is then re-used for subsequent accesses by that thread, to avoid allocation overhead. */

	public AccessEvent(ShadowThread td) {
		super(td);
	}

	/** Update the shadow location, returning true if atomic test-and-set succeeds. */
	public abstract boolean putShadow(ShadowVar newShadow);	

	/** Get current shadow value.  May be different than originalShadow, if interference happens. */
	public abstract ShadowVar getShadow();

	/** Get information about this syntactic access. */
	public abstract AccessInfo getAccessInfo();

	/** Returns the kind of this event. */
	public abstract Kind getKind(); 

	/** Returns the target field. */
	public Object getTarget() {
		return target;
	}

	/** @RRInternal */
	public void setTarget(Object target) {
		this.target = target;
	}

	/** Returns the isWrite field. */
	public boolean isWrite() {
		return isWrite;
	}

	/** @RRInternal */
	public void setWrite(boolean isWrite) {
		this.isWrite = isWrite;
	}

	/** Returns the state field, right before event was dispatched. */
	public ShadowVar getOriginalShadow() {
		return originalShadow;
	}

	/** @RRInternal */
	public void putOriginalShadow(ShadowVar newOriginalShadow) {
		this.originalShadow = newOriginalShadow;
	}

}
