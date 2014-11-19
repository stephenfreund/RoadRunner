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

package acme.util.count;


/**
 * A counter that works like a stopwatch.  
 * Call start followed by stop, and it adds the interval to the total time.
 * Can be used multiple times in a row.
 */
final public class Timer extends AbstractCounter {

	private long totalTime;
	private int count = 0;
	
	public Timer(String group, String name) {
		super(group, name);
		totalTime = 0;
	}
	
	public Timer(String name) {
		this(null, name);
	}
	
	public final long start() {
		return System.nanoTime(); 
	}

	final public synchronized long stop(long startTime) {
		long endTime = System.nanoTime();
		long elapsed = endTime - startTime;
		totalTime += elapsed;
		count++;
		return elapsed;
	}

	@Override
	public String get() {
		double totalTime = (this.totalTime) / 1000000;
		if (count > 0) {
			return String.format("<total>%g</total> <count>%d</count> <ave>%g</ave>", totalTime, count, totalTime / count);
		} else {
			return String.format("<total>%g</total> <count>%d</count> ", totalTime, count);
		}
	}
}


