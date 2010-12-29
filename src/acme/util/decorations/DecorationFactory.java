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
import java.util.HashMap;

import acme.util.Assert;

/**
 * A factory to generate Decorations for a specific type of Decoratable object.
 */
public class DecorationFactory<T extends Decoratable> implements Serializable {

	/** 
	 * Indicates whether multiple decorations can have the same name. If not
	 * only a single decoration with that name is created.
	 */
	public static enum Type { SINGLE, MULTIPLE }; 
	
	protected int allocated;
	
	private HashMap<String,Decoration<T,?>> map = new HashMap<String,Decoration<T,?>>(); 

	public DecorationFactory() {
		allocated = 0;
	}

	/** 
	 * Create a new decoration
	 * @param <V>  Type of values stored
	 * @param decorationName  Name of decoration
	 * @param type Single or Multiple occurrences with same name?
	 * @param defaultValue Value for a T object that hasn't been explicitly decorated.
	 * @return
	 */
	public synchronized <V> Decoration<T,V> make(String decorationName, Type type, DefaultValue<T,V> defaultValue) {
		if (type == Type.MULTIPLE) {
			decorationName += allocated;
		}
		Decoration<T,V> d = (Decoration<T,V>)map.get(decorationName);  // NOTE: Unsafe cast.  User may have created decoration w/same name but different type before.
		if (d == null) {
			d = new Decoration<T,V>(this, decorationName, allocated++, defaultValue);
			map.put(decorationName, d);
		} else {
			Assert.warn("Decoration '%s' previous created.", decorationName);
		}
		return d;
	}

	/** INTERNAL */
	int allocated() {
		return allocated;
	}
}
