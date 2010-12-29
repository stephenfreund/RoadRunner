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

package acme.util.decorations;

import java.io.Serializable;

/**
 * A Decoration placed on an existing structure.
 * @param <T>  Type of object that is decorated by this decoration
 * @param <V>  Type of value stored on the decoration.
 */
public final class Decoration<T extends Decoratable, V> implements Serializable {

	/** INTERNAL */
	protected final int slot;
	
	/** Name of the decoration */
	protected final String name;

	/** The default value for a T object that has not yet been decorated. */
	protected final DefaultValue<T,V> defaultValue;
	
	/** The factory that created this decoration */
	protected final DecorationFactory<T> factory;

	Decoration(DecorationFactory<T> fact, String decorationName, int slot, DefaultValue<T,V> defaultValue) {
		this.factory = fact;
		this.name = decorationName;
		this.slot = slot;
		this.defaultValue = defaultValue;
	}


	public final V get(final T n) {
		final Object[] vs=n.decorations;
		try {
			V v = (V)vs[slot];
			if (v == null) {
				set(n, v = defaultValue.get(n));
			}
			return v;
		} catch (Exception e) {
			V v = null;
			set(n, v = defaultValue.get(n));
			return v;
		}
	}


	public final void set(final T n, final V val) {
		try {
			final Object[] v = n.decorations;
			v[slot] = val;
		} catch(Exception e) {
			Object[] v = n.decorations;
			if (slot >= v.length) {
				Object[] _new = new Object[factory.allocated()];
				System.arraycopy( v, 0, _new, 0, v.length );
				v = n.decorations = _new;
			}
			v[slot] = val;
		}
	}


	@Override
	public String toString() {
		return name;
	}

}
