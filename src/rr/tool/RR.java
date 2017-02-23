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

package rr.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.CompilationMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import rr.RRMain;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.instrument.Instrumentor;
import rr.loader.Loader;
import rr.meta.MetaDataInfoMaps;
import rr.simple.LastTool;
import rr.state.ShadowThread;
import rr.tool.tasks.CountTask;
import rr.tool.tasks.GCRunner;
import rr.tool.tasks.MemoryStatsTask;
import rr.tool.tasks.ThreadStacksTask;
import rr.tool.tasks.TimeOutTask;
import acme.util.Assert;
import acme.util.StringMatchResult;
import acme.util.StringMatcher;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.count.Counter;
import acme.util.io.SplitOutputWriter;
import acme.util.io.URLUtils;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;
import acme.util.time.TimedStmt;

public class RR {


	public static final CommandLineOption<String> toolPathOption = CommandLine.makeString("toolpath", Util.getenv("RR_TOOLPATH",""), CommandLineOption.Kind.STABLE, "The class path used to find RoadRunner tools specified.");
	public static final CommandLineOption<String> classPathOption = CommandLine.makeString("classpath", ".", CommandLineOption.Kind.STABLE, "The class path used to load classes from the target program.");

	public static CommandLineOption<String> toolOption = 
			CommandLine.makeString("tool", "rrtools.simple.EmptyTool", CommandLineOption.Kind.STABLE, "The tool chain to use.  Can be single tool, sequentially composed tools, parallel composed tools, or parenthesized chain.  " + 
					"Specified with full class names or abbrevations in rr.props files on the toolpath.  " +
					"Examples: \n   -tool=FT\n   -tool=TL:V\n  -tool=rrtools.fastrack.FastTrack\n  -tool=FT|HB\n  -tool=FT:(P|V)", 
					new Runnable() { public void run() { RR.createTool(); } } );

	public static CommandLineOption<Boolean> printToolsOption = 
			CommandLine.makeBoolean("tools", false, CommandLineOption.Kind.STABLE, "Print all known tools", 
					new Runnable() { public void run() { RR.printAbbrevs(); } } );

	public static CommandLineOption<Boolean> noxmlOption = 
			CommandLine.makeBoolean("noxml", false, CommandLineOption.Kind.STABLE, "Turn off printing the xml summary at the end of the run.");

	public static CommandLineOption<Boolean> forceGCOption = 
			CommandLine.makeBoolean("constantGC", false, CommandLineOption.Kind.EXPERIMENTAL, "Turn on constant garbage collection.",
					new GCRunner());

	public static CommandLineOption<Boolean> nofastPathOption = 
			CommandLine.makeBoolean("noFP", false, CommandLineOption.Kind.STABLE, "Do not use in-lined tool fastpath code for reads/writes.");

	public static CommandLineOption<Boolean> noShutdownHookOption = 
			CommandLine.makeBoolean("noShutdownHook", false, CommandLineOption.Kind.EXPERIMENTAL, "Don't register shutdown hook.");

	public static CommandLineOption<Boolean> noEnterOption = 
			CommandLine.makeBoolean("noEnter", false, CommandLineOption.Kind.STABLE, "Do not generate Enter and Exit events.");

	public static CommandLineOption<String> xmlFileOption = 
			CommandLine.makeString("xml", "log.xml", CommandLineOption.Kind.STABLE, "Log file name for the xml summary printed at the end of the run.");

	public static CommandLineOption<String> pulseOption =
			CommandLine.makeString("pulse", "", CommandLineOption.Kind.EXPERIMENTAL, "Install periodic tasks (stacks,stats,counts).  Example: -pulse=stacks:counts", new Runnable() { public void run() { RR.createTasks(); } } );

	public static CommandLineOption<Integer> timeOutOption =
			CommandLine.makeInteger("maxTime", 0, CommandLineOption.Kind.STABLE, "Maximum execution time in seconds.",
					new Runnable() { public void run() { Util.addToPeriodicTasks(new TimeOutTask()); } } );

	public static CommandLineOption<Long> memMaxOption = CommandLine.makeLong("maxMem", (10 * 1024), CommandLineOption.Kind.STABLE, "Maximum memory in MB.");

	public static CommandLineOption<Integer> maxTidOption = 
	    CommandLine.makeInteger("maxTid", Math.max(16,Runtime.getRuntime().availableProcessors()), CommandLineOption.Kind.STABLE, "Maximum number of active threads.");

	public static CommandLineOption<Boolean> stackOption = 
			CommandLine.makeBoolean("stacks", false, CommandLineOption.Kind.STABLE, "Record stack traces for printing in erros messages.  Stacks are expensive to compute, so by default RoadRunner doesn't (See ShadowThread.java).");

	public static CommandLineOption<Boolean> valuesOption = 
			CommandLine.makeBoolean("values", false, CommandLineOption.Kind.EXPERIMENTAL, "Pass tools.internal/new values for writes to tools.  Tools can then change the new value to be written.  You MUST run java with -noverify if you use -values in conjunction with array instrumentation.");

	public static final CommandLineOption<Boolean> noTidGCOption = 
			CommandLine.makeBoolean("noTidGC", false, CommandLineOption.Kind.EXPERIMENTAL, "Do not reuse the tid for a thread that has completed.");

	public static final CommandLineOption<Boolean> noEventReuseOption = 
			CommandLine.makeBoolean("noEventReuse", false, CommandLineOption.Kind.EXPERIMENTAL, "Turn of Event Reuse.");

	public static final CommandLineOption<Boolean> trackMemoryUsageOption = 
			CommandLine.makeBoolean("trackMemoryUsage", false, CommandLineOption.Kind.EXPERIMENTAL, "Install monitors on GC to track Memory Usage",
					new Runnable() { 
				public void run() { 
					final MemoryMXBean bean = ManagementFactory.getMemoryMXBean();

					NotificationListener listener = new javax.management.NotificationListener() {
						public void handleNotification(Notification notification, Object handback)  {
							updateMemoryUsage(bean);
						}
					};

					for (GarbageCollectorMXBean gc: ManagementFactory.getGarbageCollectorMXBeans()) {
						Util.log("Adding Memory Tracking Listener to GC " + gc.getName());
						NotificationEmitter emitter = (NotificationEmitter) gc;
						emitter.addNotificationListener(listener, null, null);
					}
				} } );					



	public static final StringMatcher toolCode = new StringMatcher(StringMatchResult.REJECT, "+acme..*", "+rr..*", "+java..*");

	private static volatile boolean shuttingDown = false;
	private static boolean timeOut = false;

	private static long startTime;
	private static long endTime;

	private static Tool tool;

	protected static Tool firstEnter, firstExit, firstAcquire, firstRelease, firstAccess; //, nextArrayAccess;

	private static ToolLoader toolLoader;

	public static void createDefaultToolIfNecessary() {
		// default values, in case no tool change is supplied.
		if (getTool() == null) {
			setTool(new LastTool("Last Tool", null));
			firstEnter = firstExit = firstAcquire = firstRelease = firstAccess = getTool(); // nextArrayAccess = tool;
		}
	}

	private static void printAbbrevs() {
		try {
			URL[] path = URLUtils.getURLArrayFromString(System.getProperty("user.dir"), toolPathOption.get());
			Util.logf("Creating tool loader with path %s", java.util.Arrays.toString(path));
			toolLoader = new ToolLoader(path);
			Util.log(toolLoader.toString());
			Util.exit(0);
		} catch (Exception e) {
			Assert.panic(e);
		}

	}

	public static void initToolLoader() {
		if (toolLoader == null) {
			final URL[] urls = URLUtils.getURLArrayFromString(System.getProperty("user.dir"), toolPathOption.get());
			Util.logf("Creating tool loader with path %s", java.util.Arrays.toString(urls));
			toolLoader = new ToolLoader(urls);
		}
	}

	private static void createTool() { 
		try {
			Util.log(new TimedStmt("Creating Tool Chain") {

				protected Tool createChain(String methodName) {
					Tool t = getTool().findFirstImplementor(methodName);
					Util.logf("  %10s chain: %s", methodName, getTool().findAllImplementors(methodName));
					return t;
				}

				@Override
				public void run() throws Exception {
					initToolLoader();
					setTool(rr.tool.parser.parser.build(toolLoader, toolOption.get(), toolOption.getCommandLine()));
					Util.logf("    complete chain: %s", getTool().toChainString());

					firstEnter = createChain("enter");
					firstExit = createChain("exit");
					firstAcquire = createChain("acquire");
					firstRelease = createChain("release");
					firstAccess = createChain("access");

					if (!nofastPathOption.get()) {
						List<Tool> readFP = getTool().findAllImplementors("readFastPath");
						List<Tool> writeFP = getTool().findAllImplementors("writeFastPath");
						List<Tool> arrayReadFP = getTool().findAllImplementors("arrayReadFastPath");
						List<Tool> arrayWriteFP = getTool().findAllImplementors("arrayWriteFastPath");
						List<Tool> fieldReadFP = getTool().findAllImplementors("fieldReadFastPath");
						List<Tool> fieldWriteFP = getTool().findAllImplementors("fieldWriteFastPath");
						if (readFP.size() + writeFP.size() + arrayReadFP.size() + arrayWriteFP.size() + fieldReadFP.size() + fieldWriteFP.size() == 0) {
							Util.log("No fastpath code found");
							nofastPathOption.set(true);
						} else {
							Util.log("Read Fastpath code found in " + readFP);						
							Util.log("Write Fastpath code found in " + writeFP);						
							Util.log("Array Read Fastpath code found in " + arrayReadFP);						
							Util.log("Array Write Fastpath code found in " + arrayWriteFP);				
							Util.log("Field Read Fastpath code found in " + fieldReadFP);						
							Util.log("Field Write Fastpath code found in " + fieldWriteFP);						
						}
					} else {
						Util.log("User has disabled fastpath instrumentation.");
					}
					noEnterOption.set(noEnterOption.get() || (firstEnter.getClass() == LastTool.class && firstExit.getClass() == LastTool.class));
					if (noEnterOption.get()) {
						Util.log("No Tools implement enter/exit hooks.");
					}
				}
			}
					); 
		} catch (Exception e) {
			Assert.panic(e);
		}
	}

	public static void applyToTools(ToolVisitor tv) {
		getTool().accept(tv);
	}

	public static void startUp() {

		/* 
		 * Set a default handler to bail quickly if we run out of memory.
		 * Otherwise, the shutdown routine will run and sometimes hang due to 
		 * lack of memory.
		 */
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				if (e instanceof OutOfMemoryError) {
					try {
						System.err.println("## Out of Memory");
					} finally {
						Runtime.getRuntime().halt(17);
					}
				} else {
					Assert.panic(e);
				}
			}
		});

		Runtime.getRuntime().addShutdownHook(new Thread("RR Shutdown") {
			@Override
			public void run() {
				shutDown();
			}
		});

		Util.log("Tool Init()");

		applyToTools(new ToolVisitor() {
			public void apply(Tool t) {
				t.init();
			}
		});

	}

	public static void shutDown() {
		if (shuttingDown) return;
		shuttingDown = true;
		if (endTime == 0) {
			endTimer(); // call here in case the target didn't exit cleanly
		}

		// always always always print time
		boolean tmp = Util.quietOption.get();
		Util.quietOption.set(false);
		Util.logf("Total Time: %d", (endTime - startTime));
		Util.quietOption.set(tmp);

		if (RR.noShutdownHookOption.get()) {
			return;
		}

		if (false) {
			shortXML();
			return;
		} else {
			xml();
		}
		Util.quietOption.set(false);
		Util.logf("Time = %d", endTime - startTime);
		final String dump = Instrumentor.dumpClassOption.get();
		if (!dump.equals("")) {
			MetaDataInfoMaps.dump(dump + "/rr.meta");
			try {
				PrintWriter pw = new PrintWriter(new FileWriter(dump + "/rr.meta.txt"));
				MetaDataInfoMaps.print(pw);
				pw.close();
			} catch (IOException e) {
				Assert.panic(e);
			}
		}
	}

	/***********/

	private static void createTasks() {
		String tasks[] = pulseOption.get().split(":");
		for (String t : tasks) {
			if (t.equals("stacks")) {
				Util.addToPeriodicTasks(new ThreadStacksTask());
			} else if (t.equals("stats")) {
				Util.addToPeriodicTasks(new MemoryStatsTask());
			} else if (t.equals("counts")) {
				Util.addToPeriodicTasks(new CountTask());
			} else {
				Assert.panic("Bad Task: " + t);
			}
		}
	}

	/******************/


	static volatile long maxMemCommitted = 0;
	static volatile long maxMemUsed = 0;
	static volatile long maxTotalMemory = 0;

	private static final double M = 1024 * 1024;

	protected static synchronized void updateMemoryUsage(final MemoryMXBean bean) {
		java.lang.management.MemoryUsage m = bean.getHeapMemoryUsage();
		maxMemCommitted = Math.max(m.getCommitted(), maxMemCommitted);
		maxMemUsed = Math.max(m.getUsed(), maxMemUsed);
		maxTotalMemory = Math.max(maxTotalMemory, Runtime.getRuntime().totalMemory());
		
//		Util.logf("   Mem Committed %d", maxMemCommitted / 1000000);
//		
//		long total = 0;
//		for (MemoryPoolMXBean x : ManagementFactory.getMemoryPoolMXBeans()) {
//			long y = x.getUsage().getCommitted() / 1000000;
//			Util.logf("      %s %d",  x.getName(), y);
//			total += y;
//		}
//		Util.logf("   Total %d", total);
	}


	/*** XML ***/

	private static final String[] systemInfo = { 
		"java.vm.version",	
		"java.vm.vendor",
		"java.vm.name",
		"java.class.path",
		"os.name",
		"os.arch",
		"os.version",
		"user.name",
		"user.dir"
	};

	private static volatile boolean inXml = false;
	private static void xml() {
		if (inXml) return;
		inXml = true;

		StringWriter sOut = new StringWriter();
		Writer outputWriter = noxmlOption.get() ? Util.openLogFile(xmlFileOption.get()) :
			new SplitOutputWriter(sOut, Util.openLogFile((xmlFileOption.get())));
		PrintWriter stringOut = new PrintWriter(outputWriter);

		final XMLWriter xml = new XMLWriter(stringOut);


		xml.push("entry");
		xml.print("date", new Date()); 
		xml.print("mode", RRMain.modeName());
		xml.print("timeout", timeOut ? "YES" : "NO");

		CommandLineOption.printAllXML(xml);

		xmlSystem(xml);

		Loader.printXML(xml);
		Counter.printAllXML(xml);
		Yikes.printXML(xml);

		applyToTools(new ToolVisitor() {
			public void apply(Tool t) {
				xml.push("tool");
				xml.print("name", t.toString());
				t.printXML(xml);
				xml.pop();			
			}
		});

		xml.print("threadCount", ShadowThread.numThreads());
		xml.print("threadMaxActive", ShadowThread.maxActiveThreads());
		xml.print("errorTotal", ErrorMessage.getTotalNumberOfErrors());
		xml.print("distinctErrorTotal", ErrorMessage.getTotalNumberOfDistinctErrors());
		ErrorMessages.xmlErrorsByMethod(xml);
		ErrorMessages.xmlErrorsByField(xml);
		ErrorMessages.xmlErrorsByArray(xml);
		ErrorMessages.xmlErrorsByLock(xml);
		ErrorMessages.xmlErrorsByFieldAccess(xml);
		ErrorMessages.xmlErrorsByErrorType(xml);
		xml.print("warningsTotal", Assert.getNumWarnings());
		xml.print("yikesTotal", Yikes.getNumYikes());
		xml.print("failed", Assert.getFailed());
		xml.print("failedReason", Assert.getFailedReason());


		xml.print("time", endTime - startTime);
		xml.pop();
		xml.close();

		Util.printf("%s", sOut.toString());
	}


	private static void shortXML() {
		if (inXml) return;
		inXml = true;

		StringWriter sOut = new StringWriter();
		Writer outputWriter = noxmlOption.get() ? Util.openLogFile(xmlFileOption.get()) :
			new SplitOutputWriter(sOut, Util.openLogFile((xmlFileOption.get())));
		PrintWriter stringOut = new PrintWriter(outputWriter);

		final XMLWriter xml = new XMLWriter(stringOut);

		xml.push("entry");
//		xml.print("date", new Date()); 
//		xml.print("mode", RRMain.modeName());
		xml.print("timeout", timeOut ? "YES" : "NO");

		xml.push("system");

		xml.print("availableProcs", Runtime.getRuntime().availableProcessors());

		MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
		updateMemoryUsage(bean);
		xml.print("memCommitted",(int)Math.ceil(maxMemCommitted/M));
		xml.print("memUsed",(int)Math.ceil(maxMemUsed/M));
		xml.print("memTotal",(int)Math.ceil(maxTotalMemory/M));
		xml.pop();	
//		Loader.printXML(xml);
//		Counter.printAllXML(xml);
//		Yikes.printXML(xml);

//		applyToTools(new ToolVisitor() {
//			public void apply(Tool t) {
//				xml.push("tool");
//				xml.print("name", t.toString());
//				t.printXML(xml);
//				xml.pop();			
//			}
//		});

		xml.print("threadCount", ShadowThread.numThreads());
		xml.print("threadMaxActive", ShadowThread.maxActiveThreads());
		xml.print("errorTotal", ErrorMessage.getTotalNumberOfErrors());
		xml.print("distinctErrorTotal", ErrorMessage.getTotalNumberOfDistinctErrors());
//		ErrorMessages.xmlErrorsByMethod(xml);
//		ErrorMessages.xmlErrorsByField(xml);
//		ErrorMessages.xmlErrorsByArray(xml);
//		ErrorMessages.xmlErrorsByLock(xml);
//		ErrorMessages.xmlErrorsByFieldAccess(xml);
//		ErrorMessages.xmlErrorsByErrorType(xml);
		xml.print("warningsTotal", Assert.getNumWarnings());
		xml.print("yikesTotal", Yikes.getNumYikes());
		xml.print("failed", Assert.getFailed());
		xml.print("failedReason", Assert.getFailedReason());


		xml.print("time", endTime - startTime);
		xml.pop();
		xml.close();

		Util.printf("%s", sOut.toString());
	}
	
	
	public static void timeOut() {
		if (!shuttingDown) {
			timeOut = true;
			Util.exit(2);
		}
	}


	private static void xmlSystem(XMLWriter xml) {
		xml.push("system");

		try {
			java.net.InetAddress localMachine =
					java.net.InetAddress.getLocalHost();	
			xml.print("host", localMachine.getHostName());
		} catch(java.net.UnknownHostException uhe) {
			xml.print("host", "unknown");
		}

		for (String s : systemInfo) {
			xml.printWithFixedWidths("name", s, -35, "value", System.getProperty(s), -15);
		}

		xml.print("availableProcs", Runtime.getRuntime().availableProcessors());

		MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
		updateMemoryUsage(bean);

		updateMemoryUsage(bean);
		xml.print("memCommitted",(int)Math.ceil(maxMemCommitted/M));
		xml.print("memUsed",(int)Math.ceil(maxMemUsed/M));
		xml.print("memTotal",(int)Math.ceil(maxTotalMemory/M));

		CompilationMXBean cbean = ManagementFactory.getCompilationMXBean();
		if (cbean!=null) xml.print("compileTime",cbean.getTotalCompilationTime());
		for (GarbageCollectorMXBean gcb : ManagementFactory.getGarbageCollectorMXBeans()) { 
			xml.printInsideScope("gc", "name",gcb.getName(), "time", gcb.getCollectionTime());
		}

		xml.pop();	
	}

	public static void startTimer() {
		startTime = System.currentTimeMillis();
	}

	public static void endTimer() {
		endTime = System.currentTimeMillis();
	}

	private static void setTool(Tool tool) {
		RR.tool = tool;
	}

	public static Tool getTool() {
		return tool;
	}

	public static ToolLoader getToolLoader() {
		initToolLoader();
		return toolLoader;
	}

	/*
	 * Return true if RR has started to shutdown and the target
	 * has officially ended.
	 */
	public static boolean targetFinished() {
		return shuttingDown;
	}
}
