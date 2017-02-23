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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Vector;

import acme.util.identityhash.WeakIdentityHashMap;
import acme.util.io.NamedFileWriter;
import acme.util.io.SplitOutputWriter;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;
import acme.util.time.PeriodicTaskStmt;
import acme.util.time.TimedExpr;
import acme.util.time.TimedStmt;


/**
 * Various utility routines.
 */
public class Util {

	static private class ThreadStatus {
		int logLevel = 0;
		int newLineCount = 0;
	}

	static final ThreadLocal<ThreadStatus> threadStatus = new ThreadLocal<ThreadStatus>() {
		@Override
		protected ThreadStatus initialValue() {
			return new ThreadStatus();
		}
	};

	public static final String ERROR_PREFIX = "## ";
	public static final CommandLineOption<Boolean> quietOption = 
		CommandLine.makeBoolean("quiet", false, CommandLineOption.Kind.STABLE, "Quiet mode.  Will not print debugging or logging messages.");

	public static CommandLineOption<Integer> logDepthOption = 
			CommandLine.makeInteger("logDepth", 100, CommandLineOption.Kind.STABLE, "Ignore log messages greater than this nesting depth.");
	
	public static CommandLineOption<String> outputPathOption = 
		CommandLine.makeString("logs", "log", CommandLineOption.Kind.STABLE, "The path to the directory where log files will be stored.");

	public static CommandLineOption<String> outputFileOption = 
		CommandLine.makeString("out", "", CommandLineOption.Kind.STABLE, "Log file name for Util.out.",
				new Runnable() { public void run() {  
					String errFile = errorFileOption.get();
					String outFile = outputFileOption.get();
					if (errFile.equals(outFile)) {
						Util.out = Util.err;
					} else {
						Assert.assertTrue(outFile.length() > 0, "Bad File");
						setOut(new PrintWriter(new SplitOutputWriter(out, Util.openLogFile(outFile)), true));
					} 
				} } );

	public static CommandLineOption<String> errorFileOption = 
		CommandLine.makeString("err", "", CommandLineOption.Kind.STABLE, "Log file name for Util.err.",
				new Runnable() { public void run() {  
					String errFile = errorFileOption.get();
					String outFile = outputFileOption.get();
					if (errFile.equals(outFile)) {
						Util.err = Util.out;
					} else {
						Assert.assertTrue(errFile.length() > 0, "Bad File");
						setErr(new PrintWriter(new SplitOutputWriter(err, Util.openLogFile(errFile)), true));
					}
				} } );
	

	/**
	 * Print a message to the err stream, preceeded by ##
	 */
	public static void error(String format, Object... args) {
		synchronized(Util.class) {
			Util.err.println(String.format(ERROR_PREFIX + format, args).replaceAll("\n", "\n" + ERROR_PREFIX));
		}
	}

	/**
	 * Print a message to the err stream, preceeded by ##
	 */
	public static void error(Object o) {
		error("%s", o);
	}

	/**
	 * Print a message to the out stream.
	 */
	public static void printf(String format, Object... args) {
		synchronized(Util.class) {
			pad();
			String msg = String.format(format, args);
			if (!msg.endsWith("\n")) {
				msg += "\n";
			}
			Util.out.printf("%s", msg);
		}
	}

	/**
	 * Print a message to the out stream.
	 */
	public static void println(Object s) {
		Util.printf("%s\n", s);
	}

	/**
	 * Indent a message, given what has been previously printed by that thread.
	 */
	static void pad() {
		Util.ThreadStatus status = Util.threadStatus.get();
		if (status.newLineCount != 0) {
			out.println();
		}
		status.newLineCount = 0;
		for (int i = 0; i < status.logLevel; i++) {
			out.print("  ");
		}
	}

	/**
	 * Run the timed expression, reporting how long it took.
	 */
	public static <T> T log(TimedExpr<T> lo) throws Exception {
		ThreadStatus status = threadStatus.get();
		log(lo.toString());
		status.logLevel++;
		long time = System.currentTimeMillis();
		try {
			return lo.run();
		} finally {
			status.logLevel--;
			logf("%.3g sec",(System.currentTimeMillis() - time) / 1000.0);
		}
	}

	/**
	 * Run the timed statement, reporting how long it took.
	 * Returns time in milliseconds.
	 */
	public static long log(TimedStmt lo) throws Exception {
		ThreadStatus status = threadStatus.get();
		log(lo.toString());
		status.logLevel++;
		long time = System.currentTimeMillis();
		long d;
		try {
			lo.run();
		} finally {
			status.logLevel--;
			d = (System.currentTimeMillis() - time) ;
			logf("%.3g sec",d / 1000.0);
		}
		return d;
	}

	/**
	 * Run the timed expression, but don't repot the time.
	 */
	public static <T> T eval(TimedExpr<T> lo) throws Exception {
		ThreadStatus status = threadStatus.get();
		log(lo.toString());
		status.logLevel++;
		try {
			return lo.run();
		} finally {
			status.logLevel--;			
		}
	}

	/**
	 * Run the timed statement, but don't report the time.
	 */
	public static void eval(TimedStmt lo) throws Exception {
		ThreadStatus status = threadStatus.get();
		log(lo.toString());
		status.logLevel++;
		try {
			lo.run();
		} finally {
			status.logLevel--;			
		}
	}


	private static String prefix() {
		String prefix = Thread.currentThread().getName();
		if (prefix.equals("")) {
			prefix = "";
		} else {
			prefix += ": ";
		}
		return prefix;
	}

	/**
	 * Log to out, unless -quiet is specified.
	 */
	public static void logf(String s, Object... ops) {
		ThreadStatus status = threadStatus.get();
		if (quietOption.get() || logDepthOption.get() < status.logLevel) {
			return;
		}
		synchronized(Util.class) {
			pad();
			out.printf("[" + prefix() + s + "]\n", ops);
		}
	}

	/**
	 * Log to out, unless -quiet is specified.
	 */
	public static void log(String s) {
		logf("%s", s);
	}

	/**
	 * Log to out, unless -quiet is specified.
	 */
	public static void log(Object o) {
		log(o == null ? "null" : o.toString());
	}

	/**
	 * Log to out, unless -quiet is specified.  Add a new line afterword 8 calls to newline.  Used in Monitor.
	 */
	public static void lognl(String s) {
		ThreadStatus status = threadStatus.get();
		if (quietOption.get() || logDepthOption.get() < status.logLevel) {
			return;
		}
		synchronized(Util.class) {
			if (status.newLineCount == 0) {
				pad();
			} else if (status.newLineCount == 8) {
				pad();
			}
			status.newLineCount++;
			out.print(s);
		}
	}

	/**
	 * Log to out, regardless of whether -quiet is on.
	 */
	public static void message(String s, Object... ops) {
		ThreadStatus status = threadStatus.get();
		synchronized(Util.class) {
			pad();
			out.printf("[" + prefix() + s + "]\n", ops);
		}
	}


	/*******/

	private static WeakIdentityHashMap<Object,String> ids = new WeakIdentityHashMap<Object,String>();
	private static int idCounter = 1;
	
	/**
	 * Return identity string for any object.
	 */
	public static String objectToIdentityString(Object target) {
		if (target == null) return "null";
		if (false) { // set to true for more detailed identity info
			return String.format("0x%08X (%s)", Util.identityHashCode(target), target.getClass());
		} else {
			synchronized(Util.class) {
				String x = ids.get(target);
				if (x == null) {
					x = String.format("@%02X", idCounter++);
					ids.put(target, x);
				}
				return x;
			}
		}
	}

	/**
	 * Return printable value string for any boxed value or object.
	 */
	public static String boxedValueToValueString(Object x) {
		if (x == null) {
			return "null";
		} else if (x instanceof Number || x instanceof Boolean) {
			return x.toString();
		} else if (x instanceof Character) {
			return "'" + x + "'";
		} else if (x instanceof String) {
			return objectToIdentityString(x) + "(\"" + x + "\")";
		} else {
			return objectToIdentityString(x);
		}
	}

	/**
	 * Return the identity hashcode for an object.
	 */
	public static int identityHashCode(Object o) {
		return System.identityHashCode(o);
	}

	/******************/
 
	/**
	 * Get an environment variable, or the defaultVal if it is not defined.
	 */
	public static String getenv(String name, String defaultVal) {
		String p = System.getenv(name);
		if (p == null) {
			p = defaultVal;
		}
		return p;
	}


	/*****************/

	private static class SyncPrintWriter extends PrintWriter {

		public SyncPrintWriter(PrintStream out) {
			super(out, true);
			this.lock = Util.class;
		}

		public SyncPrintWriter(Writer out) {
			super(out, true);
			this.lock = Util.class;
		}



	}

	/** Do not modify directly. */
	static PrintWriter out;

	/** Do not modify directly. */
	static PrintWriter err;

	/**
	 * Redirect out.
	 */
	static public void setOut(PrintWriter out) {
		Util.out = new SyncPrintWriter(out);
	}

	/**
	 * Redirect err.
	 */
	static public void setErr(PrintWriter err) {
		Util.err = new SyncPrintWriter(err);
	}

	static {
		err = new SyncPrintWriter(System.err);
		out = new SyncPrintWriter(System.out);
	}

	/******************/

	private static HashMap<String, Integer> counter = new HashMap<String, Integer>();

	/**
	 * Method to generate sequenced file names.
	 */
	public static synchronized String nextFileNameInSeries(String prefix, String suffix) {
		Integer i = counter.get(prefix);
		if (i == null) {
			i = 0;
		}
		String res = prefix + i + suffix;
		counter.put(prefix, i+1);
		return res;
	}

	/******************/

	/**
	 * Method to generate sequenced file names.
	 */
	public static String makeLogFileName(String relName) {
		new File(outputPathOption.get()).mkdirs();
		String path = outputPathOption.get();
		if (!path.equals("") && path.charAt(path.length() - 1) != File.separatorChar) {
			path += File.separatorChar;
		}
		return path + relName;
	}

	/**
	 * Open a file, at the end of the output path option.
	 */
	static public NamedFileWriter openLogFile(String name) {
		try {	
//			new File(outputPathOption.get()).mkdirs();
			return new NamedFileWriter(makeLogFileName(name));
		} catch (IOException e) {
			Assert.fail(e);
			return null;
		}
	}		

	/******************/

	private static final Vector<PeriodicTaskStmt> periodicTasks = new Vector<PeriodicTaskStmt>();

	/**
	 * Install a periodically running task.
	 */
	public static void addToPeriodicTasks(PeriodicTaskStmt s) {
		periodicTasks.add(s);
		if (periodicTasks.size() == 1) periodic.start();
	}

	private static Thread periodic = new Thread(new Runnable() {
		public void run() {
			while (true) {
				try {
					Thread.sleep(1000);
					Util.log(new TimedStmt("Periodic Tasks") {
						@Override
						public void run() {
							for (int i = 0; i < periodicTasks.size(); i++) {
								PeriodicTaskStmt p = periodicTasks.get(i);
								if (p.wantsToRunTask()) {
									p.runTask();
								}
							}
						}
					});
				} catch (OutOfMemoryError e) {
					System.err.println("## Out of Memory");
					Runtime.getRuntime().halt(17);
				} catch (Exception e) {
					Assert.panic(e);
				}

			}
		}
	},
	"Periodic Tasks");


	/******************/
	private static Vector<TimedStmt> runQueue = new Vector<TimedStmt>();

	private static boolean runningQueueAlready = false;
	private static int THREADS = 1;

	
	/**
	 * Add a routine to run when runtime exits.
	 */
	public static void addToExitRunQueue(TimedStmt s) {
		runQueue.add(s);
	}

	private static void runExitQueue() {
		Thread ts[] = new Thread[THREADS];
		for (int i = 0; i < THREADS; i++) {
			(ts[i] = new Thread(new Runnable() {
				public void run() {
					while (true) {
						TimedStmt st = null;
						synchronized (runQueue) {
							if (runQueue.size() == 0) {
								return;
							}
							st = runQueue.remove(0);
						}
						try {
							st.run();
						} catch (Exception e) {
							Assert.panic(e);
						}
					}
				}})).start();
		}
		for (Thread t : ts) {
			try {
				t.join();
			} catch (InterruptedException e) {
				Assert.fail(e);
			}
		}  
	}	

	/**
	 * Call this instead of System.exit.
	 */
	public static void exit(int code) {
		Util.logf("Exiting: %d", code);
		if (!runningQueueAlready) {
			runningQueueAlready = true;
			runExitQueue();
			System.exit(code);
		}
	}

}
