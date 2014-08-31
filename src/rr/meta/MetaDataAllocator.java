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

package rr.meta;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import acme.util.Assert;
import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.decorations.SingletonValue;


public class MetaDataAllocator<S extends MetaDataInfo> implements Iterable<S>, Serializable {

	protected S mapById[];
	protected final ConcurrentHashMap<String, S> map = new ConcurrentHashMap<String,S>();
	protected final DecorationFactory<S> decorations = new DecorationFactory<S>();

	private static <T> T[] copyOf(T[] original, int newLength) {
		T[] copy = (T[]) java.lang.reflect.Array.newInstance(original.getClass().getComponentType(), newLength);
		System.arraycopy(original, 0, copy, 0,
				Math.min(original.length, newLength));
		return copy;
	}

	public MetaDataAllocator(S[] bogusArray) {
		mapById = copyOf(bogusArray, 128);
	}

	public synchronized S get(final String key) {
		return map.get(key);
	}

	public S get(final int id) {
		S s = mapById[id];
		return s;
	}

	public int size() {
		return map.size();
	}

	private synchronized void resize(final int n) {
		mapById = copyOf(mapById, n);
	}

	public synchronized S put(final S t) {
		if (t.id >= mapById.length) {
			resize(t.id * 2);
		}
		mapById[t.id] = t;
		return map.put(t.getKey(), t);
	}

	public <T> Decoration<S, T> makeDecoration(final String name, DecorationFactory.Type type, final DefaultValue<S, T> defaultValueMaker) {
		return decorations.make(name, type, defaultValueMaker);
	}

	public <T extends Serializable> Decoration<S, T> makeDecoration(final String name, DecorationFactory.Type type, final T defaultValue) {
		return decorations.make(name, type, new SingletonValue<S,T>(defaultValue));
	}

	public Iterator<S> iterator() {
		return map.values().iterator();
	}

}
