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

package rr.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import rr.meta.MetaDataInfo;
import rr.state.ShadowThread;
import acme.util.Assert;
import acme.util.Util;
import acme.util.collections.IntIntMap;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * An error message reporter for a specific syntactic category (field, method, etc.).  A summary 
 * of the reported errors will appear in the XML at the end of a run.
 * <p>
 * See sample tools for examples.  
 */
public class ErrorMessage<T extends MetaDataInfo> {

	private static int totalNumberOfErrors = 0;
	private static int totalNumberOfDistinctErrors = 0;

	public static CommandLineOption<Integer> maxWarnOption = 
		CommandLine.makeInteger("maxWarn", 100, CommandLineOption.Kind.STABLE, "Maximum number of warnings of each type that will be printed for a specific declaration/operation.");

	/** General name for this type of error */
	protected final String type;

	/** Number of errors reported in total. */
	protected int counter;

	/** Number of errors reported for each meta data element, kept by id. */
	protected final IntIntMap counters = new IntIntMap();

	/** Max number of errors of this type before they are suppressed in the output. */
	protected int limit = -1;  // -1 for eLimit, >= 0 for specific limit

	/**
	 * type: Generic name for this type of error.
	 */
	public ErrorMessage(String type) {
		this.type = type;
	}

	private void defaultStart(ShadowThread cur, PrintWriter tmp) {
		tmp.println();
		tmp.println("=====================================================================");
		tmp.printf("%s Error\n\n", type);
		tmp.printf("%15s: %-5d\n","Thread", cur.getTid());
	}

	private void defaultEnd(PrintWriter tmp) {
		tmp.println("=====================================================================");
	}

	/**
	 * Number of errors generated for meta data object t.
	 */
	public int count(T t) {
		return counters.get(t.getId());
	}

	public int getMax() {
		return limit == -1 ? maxWarnOption.get() : limit;
	}

	public void setMax(int limit) {
		this.limit = limit;
	}

	@Override
	public String toString() {
		return type;
	}

	/**
	 * Return true if we have not yet reached the limit of errors for
	 * the given meta data object.
	 */
	public boolean stillLooking(T t) {
		return t == null || counters.get(t.getId()) < getMax(); 
	}

	/**
	 * Report an error at the given location by the thread Thread.  Print
	 * each piece of extra info.  extraData should be pairs of names-values:
	 * <pre>
	  	  arrayErrors.error(thread, arrayAccessInfo,  
							"Guard State", 	prev, 
							"Array",		Util.objectToIdentityString(target) + "[]", "" + aae.getIndex(), 
							"Locks",		thread.getLocksHeld(), 
							"Prev Op",		prevOp+"-by-thread-"+start,  
							"Prev Op CV",	prev, 
							"Cur Op", 		curOp,
							"Cur Op CV", 	cv,
							"Stack",		ShadowThread.stackDumpForErrorMessage(thread));
		</pre>
	 */
	public synchronized void error(ShadowThread cur, T t, Object... extraData) {
		try {
			if (stillLooking(t)) {			
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				counter++;
				defaultStart(cur, pw);

				if (t != null) {
					synchronized(ErrorMessage.class) {
						if (count(t) == 0) {
							totalNumberOfDistinctErrors++;
						}
						counters.inc(t.getId());
					}
					pw.printf("%15s: %s\n","Blame", t);
					pw.printf("%15s: %d    (max: %d)\n", "Count", 
							counters.get(t.getId()), getMax());
				}
				printExtra(pw, extraData);
				defaultEnd(pw);
				Util.error(sw.toString());
				totalNumberOfErrors++;
			}
		} catch (Throwable e) {
			Assert.panic(e);
		}
	}

	private void printExtra(PrintWriter pw, Object... extra) {
		Assert.assertTrue(extra.length % 2 == 0, "Passing wrong number of info pieces to error message");
		for (int i = 0; i < extra.length; i+=2) {
			pw.printf("%15s: %s\n", extra[i], 
					extra[i+1] == null ? "null" : extra[i+1].toString().replaceAll("\n","\n                 "));
		}
	}

	/**
	 * Report an error attributed to multiple syntactic locations.
	 */
	public void multiBlameError(ShadowThread cur, Collection<T> ts, Object... extraData) {
		boolean report = false;
		for (T t : ts) {
			if (stillLooking(t)) report = true;
		}
		if (report) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			counter++;
			defaultStart(cur, pw);

			String blame = "";
			for (T t : ts) {
				if (blame.length() > 0) blame += " -- ";
				blame += t;
				counters.inc(t.getId());
			}
			pw.printf("%15s: %s\n","Blame", blame);

			printExtra(pw, extraData);
			defaultEnd(pw);
			Util.error(sw.toString());
		}
	}


	/** 
	 * Total number of errors reported.
	 */
	public static int getTotalNumberOfErrors() {
		return totalNumberOfErrors;
	}

	/**
	 * Number of syntactic elements on which errors were reported.
	 */
	public static int getTotalNumberOfDistinctErrors() {
		return totalNumberOfDistinctErrors;
	}

}
