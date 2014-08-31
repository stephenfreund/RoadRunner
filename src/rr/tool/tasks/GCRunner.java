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

/**
 * Run GC constantly -- helps for approximating crude memory usage stats.
 */
package rr.tool.tasks;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;

import rr.tool.RR;
import acme.util.Assert;
import acme.util.Util;
import acme.util.time.*;
import acme.util.count.HighWaterMark;

public final class GCRunner implements Runnable {
	
	private final HighWaterMark maxUsage = new HighWaterMark("GCRunner", "Max Usage");
	
	public void run() { 
		new Thread("GC Runner") {
			public void run() {
				MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
				CompilationMXBean cbean = ManagementFactory.getCompilationMXBean();

				long start = System.currentTimeMillis();
				while (System.currentTimeMillis() - start < 30000) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						Assert.panic(e);
					}
					gcAndStats(bean);

					
				}
				int delay = 100;
				while (true) {
					try {
						if (System.currentTimeMillis() - start > 240000 && delay == 100) {
							delay = 500;
						}
						Thread.sleep(delay);
					} catch (InterruptedException e) {
						Assert.panic(e);
					}
					gcAndStats(bean);
				}
			}

			protected void gcAndStats(final MemoryMXBean bean) {
				try {
					Util.log(new TimedStmt("Force GC") {
						@Override
						public void run() throws Exception {
							System.gc();
							long peak = 0;
							for (MemoryPoolMXBean b : ManagementFactory.getMemoryPoolMXBeans()) {
								peak += b.getPeakUsage().getUsed();
							}
							long mem = bean.getHeapMemoryUsage().getUsed();
							Util.logf("Mem Used %d\t Mem Peak %d",mem/1000000,peak/1000000);
							if (!RR.targetFinished()) 
								maxUsage.set(mem);
						}
					});
				} catch (Exception e) { 
					Assert.fail(e);
				}
				
			}
		}.start();
	}
}
