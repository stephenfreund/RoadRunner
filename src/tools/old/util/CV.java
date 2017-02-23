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

package tools.old.util;

import java.io.Serializable;

import rr.RRMain;
import tools.util.Epoch;
import acme.util.count.Counter;

/**
 * For efficiency, clock vectors are mutable, extensible functions from ShadowThread ids to ints
 */
public class CV implements Serializable {
	protected int[] a;
	private static final int FAST = 8;

	private static final Counter cvCount = new Counter("CV", "Count");
	private static final Counter cvOps = new Counter("CV", "Ops");

	public CV(CV cv) {
		int[] ca = cv.a;
		if (ca != null) {
		    makeCV(ca.length);
		    assignInternal(cv);
		}
	}

	public CV(int size) {
		if (size > 0) {
			makeCV(size);
		}
	}

	// check that a.length < len before calling.
	final public synchronized void assignInternal(CV cv) {
		int[] ca = cv.a;
		for(int i=0; i<a.length;i++) a[i]=ca[i]; 
	}

	public void makeCV(int i) {
		if (a == null) {
			if (RRMain.slowMode()) cvCount.inc();
			a = new int[i];
			for(int j=0;j<a.length; j++) a[j] = Epoch.make(j, 0); // new cv must have valid epochs!
		}
	}


	final private synchronized void resize(int len) {
		if (len < a.length) return;
		int[] b = new int[len];
		int i;
		for(i=0;i<a.length; i++) b[i]=a[i];
		for(; i<b.length; i++)   b[i] = Epoch.make(i, 0); // new cv must have valid epochs!
		a=b;
	}


	final public synchronized void assign(CV c) {
		if (RRMain.slowMode()) cvOps.inc();
		int[] ca = c.a;
		if (a.length<ca.length) this.resize(ca.length);
		if (a.length>ca.length) {
			c.resize(a.length);
			ca = c.a;
		}
		int[] thisa = this.a;
		switch (ca.length) {
			default: slowAssign(c);
			case 8: thisa[7]=ca[7];
			case 7: thisa[6]=ca[6];
			case 6: thisa[5]=ca[5];
			case 5: thisa[4]=ca[4];
			case 4: thisa[3]=ca[3];
			case 3: thisa[2]=ca[2];
			case 2: thisa[1]=ca[1];
			case 1: thisa[0]=ca[0];
			case 0:  
		}
	}

	/* Requires this.a.length <= c.a.length */
	final private void slowAssign(CV c) {
		int[] ca = c.a;
		int[] thisa = this.a;
		// iterate until thisa.length since someone may have extended ca since
		// we verified it was long enough.
		for(int i = FAST; i < thisa.length; i++) {
			thisa[i] = ca[i];
		}
	}

	
	final public synchronized void max(CV c) {
		if (RRMain.slowMode()) cvOps.inc();
		int[] ca = c.a;
		if (a.length<ca.length) this.resize(ca.length);
		if (a.length>ca.length) {
			c.resize(a.length);
			ca = c.a;
		}
		int[] thisa = this.a;
		switch (ca.length) {
			default: slowMax(c);
			case 8: if (thisa[7]<ca[7]) thisa[7]=ca[7];
			case 7: if (thisa[6]<ca[6]) thisa[6]=ca[6];
			case 6: if (thisa[5]<ca[5]) thisa[5]=ca[5];
			case 5: if (thisa[4]<ca[4]) thisa[4]=ca[4];
			case 4: if (thisa[3]<ca[3]) thisa[3]=ca[3];
			case 3: if (thisa[2]<ca[2]) thisa[2]=ca[2];
			case 2: if (thisa[1]<ca[1]) thisa[1]=ca[1];
			case 1: if (thisa[0]<ca[0]) thisa[0]=ca[0];
			case 0:  
		}
	}

	/* Requires this.a.length <= c.a.length */
	final private void slowMax(CV c) {
		int[] ca = c.a;
		int[] thisa = this.a;
		// iterate until thisa.length since someone may have extended ca since
		// we verified it was long enough.
		for(int i = FAST; i < thisa.length; i++) {
			if (thisa[i] < ca[i]) thisa[i] = ca[i];
		}
	}

	/* Return true if any entry in c1 is greater than in c2. */
	final public boolean anyGt(CV other) {
		if (RRMain.slowMode()) cvOps.inc();
		if (other.a.length< this.a.length) other.resize(this.a.length);
		synchronized(this) {
			int ca1[] = this.a;
			int ca2[] = other.a;
			switch (ca1.length) {  
				default: if (slowAnyGt(this,other)) return true;
				case 8:  if (ca1[7]>ca2[7]) return true;
				case 7:  if (ca1[6]>ca2[6]) return true;
				case 6:  if (ca1[5]>ca2[5]) return true;
				case 5:  if (ca1[4]>ca2[4]) return true;
				case 4:  if (ca1[3]>ca2[3]) return true;
				case 3:  if (ca1[2]>ca2[2]) return true;
				case 2:  if (ca1[1]>ca2[1]) return true;
				case 1:  if (ca1[0]>ca2[0]) return true;
				case 0:
			}
			return false;
		}
	}

	/* 
	 * Return true if any entry in c1 is greater than in c2. 
	 * Requires c2.a.length >= c1.a.length 
	 */
	final private static boolean slowAnyGt(CV c1,CV c2) {
		int ca1[] = c1.a;
		int ca2[] = c2.a;
		for(int i=FAST; i < ca1.length; i++) { 
			if (ca1[i]>ca2[i]) return true;
		}
		return false; 
	}

	/*
	 * Returns next index i>=start such that c1.a[i]>c2.a[i],
	 * or -1 if no such. 
	 */
	final public int nextGt(CV other, int start) {
		if (other.a.length<this.a.length) other.resize(this.a.length);
		synchronized(this) {
			for(int i=start; i<this.a.length; i++) {
				if(this.a[i]>other.a[i]) return i;
			}
		}
		return -1;
	}

	final public void inc(int tid) {
		if (a.length <= tid) resize(tid +1);
		a[tid]++;
	}

	final public void inc(int tid, int amount) {
		if (a.length <= tid) resize(tid + 1);
		a[tid]+=amount;
	}

	@Override
	public String toString() {
		String r = "[";
		for(int i=0; i<a.length; i++) r += (i > 0 ? " " : "") + Epoch.toString(a[i]); //String.format("%08X", a[i]);
		return r+"]";
	}


	final public int get(int tid) {
		if (tid < a.length) {
			return a[tid];
		} else {
			return 0;
		}
	}
	
	final public int size() {
		return a.length;
	}
	
	final synchronized public int gets(int tid) {
		if (tid < a.length) {
			return a[tid];
		} else {
			return 0;
		}
	}

	final synchronized public void set(int tid, int v) {
		if (a.length<=tid) {
			resize(tid+1); 
		}
		a[tid] = v;
	}

	final public void clear() {
		if (RRMain.slowMode()) cvOps.inc();
		for(int i=0; i<a.length;i++) a[i]=0;
	}

}

