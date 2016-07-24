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

package rr.state.update;

import rr.state.ShadowVar;
import acme.util.LockManager;
import acme.util.Util;
import acme.util.Yikes;

public abstract class SafeFieldUpdater extends UnsafeFieldUpdater {

	private static final int LOCKS = 4096;
	private static LockManager locks = new LockManager(LOCKS);

	public SafeFieldUpdater() {

	}

	/**
	 * Put newGS in to the shadow location for the associated field of object o. 
	 * If the expected guard state is not found there, don't do the update.  Return
	 * whatever the guard state is currently set to.
	 */
	@Override
	public final boolean putState(Object o, ShadowVar expectedGS, ShadowVar newGS) {
		int hash = hashCode(o);
		synchronized(locks.get(hash)) {
			try { 
				if (expectedGS == newGS) return true;
				ShadowVar current = get(o);
				if (current != expectedGS) {
					Yikes.yikes("SafeFieldUpdater: Concurrent update.");
					return false;
				} else {
					set(o, newGS);
					return true;
				}
			} catch (ClassCastException e) {
				// This happens when two different class loaders load files with the exact same name.
				acme.util.Assert.panic("Bad update cast: from: %s [%s] to %s [%s].\nFix by alpha-renaming one of the classes to be unique.", o.getClass(), loaderChain(o.getClass().getClassLoader()), this.getClass(), loaderChain(this.getClass().getClassLoader()));
			}
			return true;
		}
	}


	private String loaderChain(ClassLoader cl) {
		if (cl == null) {
			return "<System>";
		} else {
			return Util.objectToIdentityString(cl) + " -> " + loaderChain(cl.getParent());
		}
	}


	/**
	 * Compute a hash code for o's shadow field.
	 */ 
	protected int hashCode(Object o) { 
		// Fix for bug reported by Jake Roemer and Mike Bond.
		// We must call this.hashCode for static fields since there may be multiple SafeFieldUpdaters
		//   for the same static field.  That hashCode method will return a unique value
		//   for all updaters for the same static field, unlike Util.identityHashCode(this),
		//   which will return a different value for each instance.  The hashCode method
		//   is auto-generated for subclasses in GuardStateModifierCreator.
		return o == null ? this.hashCode() : Util.identityHashCode(o);
	}


}
