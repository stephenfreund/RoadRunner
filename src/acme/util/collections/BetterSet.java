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

package acme.util.collections;

import java.util.Collection;
import java.util.HashSet;

/**
 * A parameterized class that supports specific operation on sets of objects. It is organized to 
 * extend the Java HashSet class and it implements intersection, subset, and superset operations.
 * 
 * @param <T> The type of the element in the set.
 * 
 */
public class BetterSet<T> extends HashSet<T> {
	
	/**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = 1L;

	public BetterSet() {
		super();
	}
	
	public BetterSet(int size) {
		super(size);
	}

	/**
	 * Perform an intersect operation between the current set and a second specified set.
	 * 
	 * @param other A BetterSet object.
	 * @return The intersection.
	 */
	public BetterSet<T> intersect(BetterSet<T> other) {
		BetterSet<T> intersection = new BetterSet<T>(other.size());
		for (T rel : this) {
			if (other.contains(rel)) {
				intersection.add(rel);
			}
		}
		return intersection;
	}
	
	public boolean intersects(Collection<T> other) {
		for (T t : other) {
			if (this.contains(t)) {
				return true;
			}
		}
		return false;
	}
	 
	/**
	 * Check whether this BetterSet is a subset of another.
	 * 
	 * @param other A BetterSet object.
	 * @return Whether this BetterSet is a subset of another.
	 */
	public boolean isSubsetOf(BetterSet<T> other) {
		return other.containsAll(this);
	}
	
	/**
	 * Check whether this BetterSet is a superset of another.
	 * 
	 * @param other A BetterSet object.
	 * @return Whether this BetterSet is a superset of another.
	 */
	public boolean isSupersetOf(BetterSet<T> other) {
		return containsAll(other);
	}	
}
