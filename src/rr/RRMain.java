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

package rr;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import rr.instrument.Instrumentor;
import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.instrument.classes.CloneFixer;
import rr.instrument.classes.ThreadDataThunkInserter;
import rr.loader.InstrumentingDefineClassLoader;
import rr.meta.InstrumentationFilter;
import rr.replay.RRReplay;
import rr.state.AbstractArrayStateCache;
import rr.state.ArrayStateFactory;
import rr.state.ShadowThread;
import rr.state.agent.ThreadStateExtensionAgent;
import rr.state.agent.ThreadStateExtensionAgent.InstrumentationMode;
import rr.state.update.Updaters;
import rr.tool.RR;
import rr.tool.Tool;
import rr.tool.ToolVisitor;
import acme.util.Assert;
import acme.util.StackDump;
import acme.util.Util;
import acme.util.count.Counter;
import acme.util.io.URLUtils;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;
import acme.util.time.TimedStmt;

/*
 * The Main class for RoadRunner: creates tool chain, processes flags, 
 * runs the target program.
 * 
 * There are two flags to enable repeated runs of the target.  This
 * approached is inspired by the DaCapo test harness and is still
 * being refined --- feedback welcome.To use this feature, run as follows:
 *    
 *     rrrun -benchmark=10 -warmup=3 -tool=FT2 Target 
 *
 * where Target is some class that supports the following methods:
 *   - public Target(String args[])
 *   - public void preIteration()
 *   - public void iteration()
 *   - public void postIteration()
 *   - public void cleanup()
 * RR then runs the equivalent of:
 *   Target t = new Target(command-line-arguments);
 *   for (int i = 0; i < warmup; i++) {
 *     t.preIteration(); t.iteration(); t.postIteration();
 *   }
 *   for (int i = 0; i < benchmark; i++) {
 *     t.preIteration(); t.iteration(); t.postIteration();
 *   }
 *   t.cleanup()
 * Only the calls to iteration are timed.  RR runs the garbage collector 
 * between iterations.  The XML at the end of the run contains timings
 * for each iteration and also the average of the benchmark iterations.
 */
public class RRMain {

	public static final class RRMainLoader extends URLClassLoader {
		private RRMainLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		public String toString() {
			return "RRMainLoader";
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			return super.findClass(name);
		}
	}

	public static final CommandLineOption<Boolean> noInstrumentOption = 
			CommandLine.makeBoolean("noinst", false, CommandLineOption.Kind.STABLE, "Do not instrument any class files.", new Runnable() { public void run() { instrumentOption.set(InstrumentationMode.NOINST); } });

	public static final CommandLineOption<InstrumentationMode> instrumentOption = 
			CommandLine.makeEnumChoice("inst", InstrumentationMode.INST, CommandLineOption.Kind.STABLE, "Instrument mode: ISNT for instrument; NOINST for no instrument; REP for build repository", InstrumentationMode.class, 
					new Runnable() { public void run() { ThreadStateExtensionAgent.addInstrumenter(instrumentOption.get());} });

	public static final CommandLineOption<Integer> infinitelyRunningThreadsOption = 
			CommandLine.makeInteger("infThreads", 0, CommandLineOption.Kind.EXPERIMENTAL, "Number of threads that loop forever.");

	public static final CommandLineOption<Integer> benchmarkOption = 
			CommandLine.makeInteger("benchmark", 0, CommandLineOption.Kind.EXPERIMENTAL, "Benchmark...");

	public static final CommandLineOption<Integer> warmUpOption = 
			CommandLine.makeInteger("warmup", 3, CommandLineOption.Kind.EXPERIMENTAL, "Warm Up...");



	public static final CommandLineOption<Integer> availableProcessorsOption = 
			CommandLine.makeInteger("availableProcessors", 
					Runtime.getRuntime().availableProcessors(), 
					CommandLineOption.Kind.EXPERIMENTAL, 
					"Number of procs RR says the machine has.");



	public static RRMainLoader loader;

	private static volatile int runningThreads;

	private static void runTargetMain(final String className,
			final String[] argv) throws InterruptedException {
		Thread appMainThread = new Thread("main") {
			@Override
			public void run() {
				try {
					Util.message("----- ----- ----- -----       Meep Meep.      ----- ----- ----- -----");
					Util.message("");

					RR.startTimer();

					Class<?> cl = loader.findClass(className);

					Method method = method = cl.getMethod("main", new Class[] { argv.getClass() });

					/*
					 * Method main is sane ?
					 */
					int m = method.getModifiers();
					Class r = method.getReturnType();

					if (!(Modifier.isPublic(m) && Modifier.isStatic(m))
							|| Modifier.isAbstract(m) || (r != Void.TYPE)) {
						Assert.fail("In class "
								+ className
								+ ": public static void main(String[] argv) is not defined");

					}
					method.invoke(null, new Object[] { argv });
					ShadowThread.terminate(this);

					runFini();

					RR.endTimer();

					Util.message("");
					Util.message("----- ----- ----- -----      Thpthpthpth.     ----- ----- ----- -----");
				} catch (Exception e) {
					//	e.printStackTrace();
					Assert.panic(e);
				}
			}
		};
		appMainThread.start();
		appMainThread.join();
	}

	protected static void runFini() {
		Util.log("Tool Fini()");
		RR.applyToTools(new ToolVisitor() {
			public void apply(Tool t) {
				t.fini();
			}
		});
	}


	public static int processArgs(String argv[]) {

		final CommandLine cl = new CommandLine("rrrun", "MainClass/EventLog");

		cl.add(new CommandLineOption<Boolean>("help", false, false, CommandLineOption.Kind.STABLE, "Print this message.") {
			@Override
			protected void apply(String arg) {
				Util.error("\n\nEnvironment Variables");
				Util.error("---------------------");
				Util.error("  RR_MODE        either FAST or SLOW.  All asserts, logging, and debugging statements\n" +
						"                 should be nested inside a test ensuring that RR_MODE is SLOW.");
				Util.error("  RR_META_DATA   The directory created on previous run by -dump from which to reload\n" +
						"                 cached metadata and instrumented class files.\n");
				cl.usage();
				Util.exit(0);
			}
		});

		cl.addGroup("General");

		cl.add(rr.tool.RR.classPathOption); 
		cl.add(rr.tool.RR.toolPathOption); 
		cl.add(rr.tool.RR.toolOption);
		cl.add(rr.tool.RR.printToolsOption); 

		cl.add(rr.loader.LoaderContext.repositoryPathOption);

		cl.addGroup("Benchmarking");
		cl.add(benchmarkOption);
		cl.add(warmUpOption);

		cl.addGroup("Instrumentor");
		cl.add(noInstrumentOption); 
		cl.add(instrumentOption); 
		cl.add(rr.tool.RR.nofastPathOption);
		cl.add(InstrumentationFilter.classesToWatch);
		cl.add(InstrumentationFilter.fieldsToWatch);
		cl.add(InstrumentationFilter.methodsToWatch);
		cl.add(InstrumentationFilter.linesToWatch);
		cl.add(InstrumentationFilter.methodsSupportThreadStateParam);
		cl.add(InstrumentationFilter.noOpsOption);
		cl.add(rr.tool.RR.valuesOption);
		cl.add(ThreadDataThunkInserter.noConstructorOption);
		cl.add(CloneFixer.noCloneOption);
		cl.add(rr.tool.RR.noEnterOption);
		cl.add(rr.tool.RR.noShutdownHookOption);
		cl.add(Instrumentor.dumpClassOption);
		cl.add(InstrumentingDefineClassLoader.sanityOption);
		cl.add(Instrumentor.fancyOption);
		cl.add(Instrumentor.verifyOption);
		cl.add(Instrumentor.trackArraySitesOption);
		cl.add(Instrumentor.trackReflectionOption);
		cl.add(ThreadStateExtensionAgent.noDecorationInline);
		cl.addOrderConstraint(ThreadStateExtensionAgent.noDecorationInline, rr.tool.RR.toolOption);


		cl.addGroup("Monitor");
		cl.add(rr.tool.RR.xmlFileOption);
		cl.add(rr.tool.RR.noxmlOption);
		cl.add(rr.tool.RR.stackOption); 
		cl.add(rr.tool.RR.pulseOption);
		cl.add(rr.tool.RR.noTidGCOption);
		cl.add(rr.tool.RREventGenerator.noJoinOption);  
		cl.add(rr.tool.RREventGenerator.indicesToWatch);  
		cl.add(rr.tool.RREventGenerator.multiClassLoaderOption);  
		cl.add(rr.tool.RR.forceGCOption);
		cl.add(Updaters.updateOptions);
		cl.add(ArrayStateFactory.arrayOption);
		cl.add(Instrumentor.fieldOption);
		cl.add(rr.barrier.BarrierMonitor.noBarrier);
		cl.add(RR.noEventReuseOption);
		cl.add(AbstractArrayStateCache.cacheTypeOption);
		cl.add(infinitelyRunningThreadsOption);
		cl.add(rr.instrument.methods.ThreadDataInstructionAdapter.callSitesOption);
		cl.add(rr.tool.RR.trackMemoryUsageOption);

		cl.addGroup("Limits");
		cl.add(rr.tool.RR.timeOutOption);
		cl.add(rr.tool.RR.memMaxOption);
		cl.add(rr.tool.RR.maxTidOption);
		cl.add(rr.RRMain.availableProcessorsOption);
		cl.add(rr.error.ErrorMessage.maxWarnOption); 


		cl.addOrderConstraint(rr.tool.RR.classPathOption, rr.tool.RR.toolOption);
		cl.addOrderConstraint(rr.tool.RR.toolPathOption, rr.tool.RR.toolOption);
		cl.addOrderConstraint(rr.tool.RR.toolOption, rr.tool.RR.toolOption);
		cl.addOrderConstraint(rr.barrier.BarrierMonitor.noBarrier, rr.tool.RR.toolOption);

		int n = cl.apply(argv);

		RR.createDefaultToolIfNecessary();
		ArrayStateFactory.addAtticListener();

		if (n >= argv.length) {
			Assert.fail("Missing class name. Use -help for summary of options.");
		}
		return n;
	}

	private static void waitForAllThreads() {
		while (runningThreads > infinitelyRunningThreadsOption.get()) {
			Util.logf("Waiting for Thread Count to reach %d.  Current Count: %d", infinitelyRunningThreadsOption.get(), runningThreads);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				acme.util.Assert.warn("Interrupted...");
			}
		}
	}

	public static void exit(int i) {
		if (runningThreads > infinitelyRunningThreadsOption.get()) {
			Assert.warn("System.exit called, but some forked threads are still running: expected count: %d.  current Count: %d", infinitelyRunningThreadsOption.get(), runningThreads);
		}
		runFini();
		Util.exit(i);
	}

	public static int availableProccesors(Object runtime) {
		int n = availableProcessorsOption.get();
		Util.log("Target asked for available processors.  Giving back: " + n);
		return n;
	}


	/**
	 * Default main method used as wrapper, expects the fully qualified class
	 * name of the real class as the first argument.
	 */
	public static void main(final String[] argv) {

		Thread.currentThread().setName("RR");

		/*
		 * Expects class name as first argument, other arguments are by-passed.
		 */
		final int n = processArgs(argv);

		ThreadStateExtensionAgent.addInstrumenter(instrumentOption.get());

		String urls = RR.classPathOption.get();

		Util.log("System Class Path = " + Arrays.toString(URLUtils.getURLArrayFromString(System.getProperty("user.dir"), urls)));
		loader = new RRMainLoader(URLUtils.getURLArrayFromString(System.getProperty("user.dir"), urls), RRMain.class.getClassLoader());

		RR.startUp();

		try {
			final String fileName = argv[n].replace("/", ".");
			final String[] newArgv = new String[argv.length - (n + 1)];

			System.arraycopy(argv, n + 1, newArgv, 0, newArgv.length);
			if (!fileName.endsWith(".rrlog")) {
				if (benchmarkOption.get() == 0) {
					runNormally(fileName, newArgv);
				} else {
					runBenchmark(fileName, newArgv);
				}
			} else {
				replay(fileName, newArgv);
			}
		} catch (RuntimeException e) {
			Util.log("Cleaning up after RuntimeException " + e + "...");
		} catch (OutOfMemoryError e) {
			// no much we can do but exit quickly.  Even calling panic may 
			//   require some memory or it may hang.
			System.err.println("## Out of Memory");
			Runtime.getRuntime().halt(17);
		} catch (Exception e) {
			Assert.panic(e);
		}

		RR.shutDown();	
		Util.exit(0);
	}


	private static void runNormally(final String className, final String newArgv[]) throws Exception {

		Util.log(new TimedStmt("Running target") {
			@Override
			public void run() throws Exception {
				runTargetMain(className, newArgv);
				waitForAllThreads();
			}
		});
	}

	private static void replay(final String className, final String newArgv[]) throws Exception {

		Util.log(new TimedStmt("Running Replay of " + className) {
			@Override
			public void run() throws Exception {
				RRReplay replay = new RRReplay(className);
				replay.go();
			}
		});
	}

	private static void runBenchmark(final String className, final String[] argv) throws Exception {
		final Thread appMainThread = new Thread("main") {
			Class<?> cl;
			Constructor<?> construct;
			Class<?>[] emptyParams;
			Method preIteration;
			Method iterate;
			Method postIteration;
			Method cleanup;

			Object benchmark;

			@Override
			public void run() {
				try {
					cl = loader.findClass(className);
					construct = cl.getConstructor(new Class[] { argv.getClass() });
					emptyParams = new Class[] { };
					preIteration = cl.getMethod("preIteration", emptyParams);
					iterate = cl.getMethod("iterate", emptyParams);
					postIteration = cl.getMethod("postIteration", emptyParams);
					cleanup = cl.getMethod("cleanup", emptyParams);

					benchmark = construct.newInstance((Object)argv);

					RR.startTimer();

					for (int i = 0; i < warmUpOption.get(); i++) {
						doOneIteration("Warmup " + (i+1));
					}

					long total = 0;
					final int iterations = benchmarkOption.get();

					for (int i = 0; i < iterations; i++) {
						total += doOneIteration("Iter " + (i+1));
					}

					cleanup.invoke(benchmark);

					Counter c = new Counter("RRBench", "Average");
					c.add(total / iterations);

					ShadowThread.terminate(this);

					runFini();

					RR.endTimer();
				} catch (Exception e) {
					//	e.printStackTrace();
					Assert.panic(e);
				}
			}

			protected long doOneIteration(String name) throws Exception {

				Util.message("");
				Util.message("");
				Util.message("");
				Util.message("----- ----- -----     Pre-Benchmark GC    ----- ----- -----", name);
				Util.message("");

				Util.log(new TimedStmt("Benchmark GC") {
					public void run() throws Exception {
						ArrayStateFactory.clearAll();
						// Twice to clear out internal RR caches and gc them.
						System.gc();
						System.gc();
					}
				});

				Util.message("----- ----- -----     Benchmark Meep Meep: %s.    ----- ----- -----", name);
				Util.message("");

				preIteration.invoke(benchmark);
				long d = Util.log(new TimedStmt("Benchmark Iteration") {
					public void run() throws Exception {
						iterate.invoke(benchmark);

						Util.log(new TimedStmt("Cleaning up Any ShadowThreads") {
							public void run() throws Exception {
								for (ShadowThread st : ShadowThread.getThreads()) {
									final Thread thread = st.getThread();
									if (thread == null || !thread.isAlive()) {
										Util.log("Thread " + st + " is not alive.  Telling Tool it has stopped.");
										st.terminate();
									}
								}
							}
						});
					
					}
				});
				postIteration.invoke(benchmark);

				Util.message("");
				Util.message("----- ----- -----     Benchmark Thpthpthpth: %d.    ----- ----- -----", d);
				Util.message("");
				Util.message("");
				Util.message("");
				Counter c = new Counter("RRBench", name);
				c.add(d);
				
				return d;
			}
		};
		Util.log(new TimedStmt("Running target in Benchmark Mode") {
			public void run() {
				try {
					appMainThread.start();
					appMainThread.join();
				} catch (Exception e) {
					Assert.panic(e);
				}
			}
		});
	}

	/***********/

	public static final int MODE_SLOW = 0;
	public static final int MODE_FAST = 1;
	private static final String modeNames[] = { "SLOW", "FAST" };

	public static final int mode;

	static {
		String m = Util.getenv("RR_MODE", "SLOW");
		if (m.equals("FAST")) {
			mode = MODE_FAST;
			Util.log("Running in FAST Mode");
		} else if (m.equals("SLOW")) {
			mode = MODE_SLOW;
			Util.log("Running in SLOW Mode");
		} else {
			Assert.fail("RR_MODE environment variable is '" + m + "'.  It must be SLOW or FAST.");
			mode = MODE_SLOW; // bogus
		}
	}

	public static final boolean fastMode() {
		return mode == MODE_FAST;
	}

	public static final boolean slowMode() {
		return mode == MODE_SLOW;
	}

	public static String modeName() {
		return modeNames[mode];
	}


	public static synchronized void incThreads() {
		runningThreads++;
	}

	public static synchronized void decThreads() {
		runningThreads--;
	}

	public static synchronized int numRunningThreads() {
		return runningThreads;
	}

}
