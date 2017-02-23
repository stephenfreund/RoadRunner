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

package rr.state;


import java.lang.ref.WeakReference;

import rr.RRMain;
import acme.util.Assert;
import acme.util.WeakResourceManager;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.count.Counter;
import acme.util.decorations.Decoratable;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;

public class ShadowLock extends Decoratable {

	public static final DecorationFactory<ShadowLock> decoratorFactory = new DecorationFactory<ShadowLock>(); 

	public static <T> Decoration<ShadowLock, T> makeDecoration(String name, DecorationFactory.Type type, DefaultValue<ShadowLock, T> defaultValueMaker) {
		return decoratorFactory.make(name, type, defaultValueMaker);
	}

	private static final Counter count = new Counter("ShadowLock", "objects");

	// Must be a Weak Ref so that our shadow state does not
	//   pin down objects that could otherwise be collected. 
	private final WeakReference<Object> lock;

	private int holdCount = 0;
	private ShadowThread curThread = null;
	private final int hashCode;

	static private int counter = 0;

	/*
	 * Constructor / helper methods.
	 */

	private ShadowLock(Object lock) { 
		// Assert.assertTrue(lock != null);
		this.lock = new WeakReference<Object>(lock);
		hashCode = counter++;
		if (RRMain.slowMode()) count.inc();
	}

	@Override
	public final int hashCode() {
		return hashCode;
	}


	public final int get() {
		//		check();	
		return holdCount;
	}

	public final ShadowThread getHoldingThread() {
		//		check();
		return curThread;
	}

	public final int inc(ShadowThread curThread) {
		if (holdCount == 0) {
			this.curThread = curThread;
		} else {
			//			if (RR.slowMode()) Util.assertTrue(this.curThread == curThread); // , "%d != %d", this.curThread.tid,  curThread.tid);
		}
		holdCount++;
		//		check();
		return holdCount;
	}

	public final int dec(ShadowThread curThread) {
		//		check();
		holdCount--;
		if (holdCount == 0) {
			this.curThread = null;
			//			check();
		} else {
			//			if (RR.slowMode()) Util.assertTrue(this.curThread == curThread); //, "%d != %d", this.curThread.tid,  curThread.tid);
		}
		//		check();
		return holdCount;
	}


	public final void set(int count, ShadowThread cur) {
		holdCount = count;
		curThread = cur;
		// check();
	}

	@Override
	public String toString() {
		return "LOCK " + Util.objectToIdentityString(this.getLock());
	}

	public void check() {
		Assert.assertTrue( (curThread == null && holdCount == 0) ||
				(curThread != null && holdCount != 0),
				"curThread:"+curThread+" holdCount:"+holdCount);
	}

	
	private static final WeakResourceManager<Object, ShadowLock> locks = new WeakResourceManager<Object, ShadowLock>() {

		@Override
		protected ShadowLock make(Object k) {
			return new ShadowLock(k);
		}
	};

	public static ShadowLock get(Object o) {
		return locks.get(o);
	}

	/**
	 * This may return null if the object has been collected.
	 */
	public Object getLock() {
		if (lock == null) return null;
		Object l = lock.get();
		if (l == null) Yikes.yikes("Getting target of ShadowLock after target has been gc'd");		
		return l;
	}

}

