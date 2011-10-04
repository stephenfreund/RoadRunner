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

package tools.util;

import rr.RRMain;
import rr.state.ShadowLock;
import rr.state.ShadowVar;
import acme.util.Assert;
import acme.util.LockManager;
import acme.util.Util;
import acme.util.count.Counter;

public final class LockSet implements ShadowVar {

	public static final Counter lockSetCounter = new Counter("LockSet", "Objects");

	static public final LockSet emptySet = new LockSet(null, 0, null, null);
	static private final LockManager tableLocks = new LockManager(4096);

	protected final int id;
	protected final int hashcode;
	public final ShadowLock lock;
	
	protected final LockSet rest;  // to smaller lock set
	protected final LockSet next;  // for table linked list

	private static int TABLE_SIZE = 523233;
	private static LockSet table[] = new LockSet[TABLE_SIZE];

	static private int numSets = 0;


	static {
		table[0] = emptySet;
	}

	final Cache intersect = new Cache();
	final Cache insert = new Cache();

	protected LockSet(ShadowLock shadowLock, int hashCode, LockSet prev, LockSet next) {
		if (RRMain.slowMode()) lockSetCounter.inc();
		this.hashcode = hashCode;
		this.rest = prev;
		this.lock = shadowLock;
		this.next = next;
		this.id = numSets++;
	}

	public ShadowLock getFirst() {
		return this.lock;
	}

	public LockSet getRest() {
		return this.rest;
	}

	public boolean isEmpty() {
		return this == emptySet;
	}

	public static LockSet emptySet() {
		return emptySet;
	}

	public static int largestSetSize() {
		int max = 0;
		for (int i = 0; i < TABLE_SIZE; i++) { 
			for (LockSet ls = table[i]; ls != null; ls = ls.next) {
				int size = 0;
				for (LockSet ls2 = ls; ls2 != null; ls2 = ls2.rest) {
					size++;
				}
				if (max < size) max = size;
			}
		}
		return max;
	}	

	public static int[] cacheSizes() {
		int a[] = new int[Cache.CACHE_SIZE + 1];
		for (int i = 0; i < TABLE_SIZE; i++) { 
			for (LockSet ls = table[i]; ls != null; ls = ls.next) {
				a[ls.intersect.size]++;
				a[ls.insert.size]++;
			}
		}
		return a;
	}

	// pre: ld not in lockset...
	public LockSet add(ShadowLock lock) {
		LockSet newLs = insert.get(lock);
		if (newLs != null) return newLs;
		int newHashCode = this.hashcode + lock.hashCode();
		int bucket = (newHashCode & 0x7fffffff) % TABLE_SIZE;
		synchronized(tableLocks.get(bucket)) {
			for (LockSet ls = table[bucket]; ls != null; ls = ls.next) {
				if (ls.rest == this && ls.lock == lock) {
					return ls;
				}
			}

			newLs = new LockSet(lock, newHashCode, this, table[bucket]);
			table[bucket] = newLs;
		}

		if (RRMain.slowMode()) lockSetCounter.inc();
		insert.put(lock, newLs);

//		Util.log(lock + " + " + this + " ==> " + newLs);

		return newLs;
	}

	public boolean contains(ShadowLock lock) {
		for (LockSet ls = this; ls != LockSet.emptySet; ls = ls.getRest()) {
			if (ls.lock == lock) {
				Util.log(this + " ? " + lock + " ==> " + true);
				return true;
			}
		}
//		Util.log(this + " ? " + lock + " ==> " + false);
		return false;
	}

	// lock must be top element
	public LockSet remove(ShadowLock lock) {
		if (RRMain.slowMode()) Assert.assertTrue(lock == this.lock);
		return this.getRest();
	}

	private static LockSet intersectRec(LockSet ls1, LockSet ls2) {
		if (ls1 == emptySet) {
			return ls1;
		}
		LockSet result = intersectRec(ls1.getRest(), ls2);
		final ShadowLock lock = ls1.getFirst();
		if (ls2.contains(lock)) {
			result = result.add(lock);
		}
		return result;
	}

	public LockSet intersect(LockSet other) {
		return intersect(this, other);
	}
	
	public static LockSet intersect(LockSet ls1, LockSet ls2) {
		if (ls1 == ls2) {
//			Util.log(ls1 + " /\\ " + ls2 + " ==> " + ls1);
			return ls1;
		}
		if (ls2.id < ls1.id) {
			LockSet tmp = ls1;
			ls1 = ls2;
			ls2 = tmp;
		}
		if (ls1 == emptySet) {
//			Util.log(ls1 + " /\\ " + ls2 + " ==> " + ls1);
			return ls1;
		}

		LockSet result = ls1.intersect.get(ls2);
		if (result != null) {
//			Util.log(ls1 + " /\\ " + ls2 + " ==> " + result);
			return result;
		}

		result = intersectRec(ls1, ls2);

		ls1.intersect.put(ls2, result);

//		Util.log(ls1 + " /\\ " + ls2 + " ==> " + result);
		return result;
	}

	@Override
	public String toString() {
		if (this == LockSet.emptySet) {
			return "[ls0]";
		}
		return "[ls" + id + ": " + Util.objectToIdentityString(lock.getLock()) + " " +  rest 
		+"]";
	}

	public static String allToString() {
		String s = "<<< ";
		for (int i = 0; i < TABLE_SIZE; i++) { 
			for (LockSet ls = table[i]; ls != null; ls = ls.next) {
				s = s + "\n    " + ls;
			}
		}
		return s + "\n>>>";
	}


	private class Cache {
		private static final int CACHE_SIZE = 16;
		Object[] key = new Object[CACHE_SIZE];
		LockSet[] ls = new LockSet[CACHE_SIZE];
		int insertCursor = 0;
		int size = 0;

		public void put(Object key, LockSet ls) {
			synchronized (tableLocks.get(hashcode)) {
				if (size < CACHE_SIZE) size++;
				this.key[insertCursor] = key;
				this.ls[insertCursor] = ls;
				insertCursor = (insertCursor + 1) % CACHE_SIZE;
			}
		}

		public LockSet get(Object key) {
			final Object[] keys = this.key;
			synchronized (tableLocks.get(hashcode)) {
				for (int i = 0, x = insertCursor;
				i < size;
				i++, insertCursor = (insertCursor + 1) % CACHE_SIZE) {
					if (keys[x] == key) {
						return ls[x];
					}
				}
			}			
			return null;
		}
	}
}

