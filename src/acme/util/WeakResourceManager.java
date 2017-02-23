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

package acme.util;

import java.lang.ref.WeakReference;
import java.util.Vector;

import acme.util.identityhash.ConcurrentIdentityHashMap;
import acme.util.identityhash.WeakIdentityHashMap;

/**
 * A generic, threadsafe lookup table that knows how to allocate an entry
 * for a key that has not been looked up before.  This uses weak refs for
 * keys so that dead keys/values are automatically reclaimed.
 */
public abstract class WeakResourceManager<K,V> {

	private final WeakIdentityHashMap<K,V> attic = new WeakIdentityHashMap<K,V>();
	private final ConcurrentIdentityHashMap<K,V> table = new ConcurrentIdentityHashMap<K,V>();

	private static final Vector<WeakReference<WeakResourceManager<?,?>>> managers = new Vector<WeakReference<WeakResourceManager<?,?>>>();

	public WeakResourceManager() {
		managers.add(new WeakReference<WeakResourceManager<?,?>>(this));
	}

	static {
		new Thread("Weak Resource Cleaner") {
			public void run() {
				while (true) {
					try {
						Thread.sleep(5000);
					} catch (Exception e) {
						Assert.panic(e);
					}
					Util.log("Cleaning...");
					
					// remove gc'd
					synchronized (managers) {
						for (int i = managers.size() - 1; i >= 0; i--) {
							if (managers.get(i).get() == null) {
								managers.remove(i);
							} 
						}
					}
					
					// clear each
					for (int i = 0; i < managers.size(); i++) {
						WeakReference<WeakResourceManager<?,?>> manager = managers.get(i);
						WeakResourceManager<?,?> ptr = manager.get();
						if (ptr != null) {
							synchronized(ptr) {
								ptr.table.clear();
							}
						}
					}
				}
			}
		}.start();
	}

	public V get(K key) {
		int hash = Util.identityHashCode(key);
		V v = table.get(key, hash);
		if (v == null) {
			synchronized (this) {
				v = table.get(key, hash);
				if (v == null) {
					v = attic.get(key);
					if (v == null) {
						v = make(key);
						attic.put(key, v);
					}
					table.put(key, v);
				}
			}
		}
		return v;
	}

	protected abstract V make(K k);

}
