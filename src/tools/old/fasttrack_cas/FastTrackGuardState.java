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

package tools.old.fasttrack_cas;

import acme.util.Util;
import acme.util.Yikes;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import sun.misc.Unsafe;
import tools.old.util.CV;
import tools.util.Epoch;
import tools.util.EpochPair;

/*
 * See FastTrack paper for description.  
 * This state encodes R/W epochs as a long and inherits and 
 * vector clock for use in ReadShared mode.
 * 
 * 
 * Synchronization Rules:
 *    - wrEpochs = (write,read) epochs.  They must be updated 
 *         atomically with cas() method
 *    - cv[i] must be accessed with lock on guard state held or
 *         a stale value may be read
 *    - can only set read epoch to READ_SHARED or move out of 
 *         READ_SHARED with lock on guard state held. 
 */

public class FastTrackGuardState extends CV implements ShadowVar {

	protected volatile long wrEpochs;

	public FastTrackGuardState() {
		super(0);
	}

	public FastTrackGuardState(boolean isWrite, int epoch) {
		super(0);
		init(isWrite, epoch);
	} 

	public FastTrackGuardState(CV cv, long epochs) {
		super(cv);
		this.wrEpochs = epochs;
	}

	public void init(boolean isWrite, int epoch) {
		if (isWrite) {
			setWREpochs(EpochPair.make(epoch, Epoch.ZERO));
		} else {
			setWREpochs(EpochPair.make(Epoch.ZERO, epoch));
		}		
	}

	@Override
	public void makeCV(int i) {
		super.makeCV(i);
	}

	@Override
	public String toString() {
		return String.format("[W=%s R=%s CV=%s]", Epoch.toString(getLastWrite()), Epoch.toString(getLastRead()), a == null ? "null" : super.toString());
	}

	protected int/*epoch*/ getLastWrite() {
		return EpochPair.write(getWREpochs());
	}

	protected int/*epoch*/ getLastRead() {
		return EpochPair.read(getWREpochs());
	}

	public boolean cas(long expected, int write, int read) {
		final boolean b = unsafe.compareAndSwapLong(this, epochsOffset, expected, EpochPair.make(write, read));
		if (!b) Yikes.yikes("Atomic updated failed.");
		return b;
	}

	public long getWREpochs() {
		return wrEpochs;
	}

	public void setWREpochs(long wrEpochs) {
		this.wrEpochs = wrEpochs;
	}

	// setup to use Unsafe.compareAndSwapLong for updates
	private static final Unsafe unsafe = Unsafe.getUnsafe();
	private static final long epochsOffset;

	static {
		try {
			epochsOffset = unsafe.objectFieldOffset(FastTrackGuardState.class.getDeclaredField("wrEpochs"));
		} catch (Exception ex) { throw new Error(ex); }
	}



}
