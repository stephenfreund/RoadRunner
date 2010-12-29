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

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import acme.util.collections.ArrayIterator;

/**
 * Methods for printring or returning a String representing the stack of a thread, excluding library entries.
 */
public class StackDump {

	
	public static String stackDump() {
		return StackDump.stackDump(Thread.currentThread(), null);
	}

	public static String stackDump(Thread thread) {
		return StackDump.stackDump(thread, null);
	}

	public static String stackDump(Thread thread, StringMatcher systemCode) {
		String res = "";
		boolean inUser = false;
		for (StackTraceElement ste: thread.getStackTrace()) {
			String n = ste.getClassName();
			if (systemCode == null || systemCode.test(n) != StringMatchResult.ACCEPT) {
				inUser = true;
			}
			if (inUser) { 
				res += ste.toString().trim();
				res += "\n";
			}
		}
		return res;
	}

	public static String stackDump(Throwable t) {
		return StackDump.stackDump(t, null);
	}

	public static String stackDump(Throwable t, StringMatcher systemCode) {
		String res = "";
		boolean inUser = false;
		if (t == null) {
			return "<no stack>";
		}
		for (StackTraceElement ste: t.getStackTrace()) {
			String n = ste.getClassName();
			if (systemCode == null || systemCode.test(n) != StringMatchResult.ACCEPT) {
				inUser = true;
			}
			if (inUser) { 
				res += ste.toString().trim();
				res += "\n";
			}
		}
		return res;
	}

	/*****************/
	
	public static void printStack(PrintWriter out) {
		StackDump.printStack(out, new Throwable(), "");
	}

	public static void printStack(PrintWriter out, Throwable e, String prefix) {
		Iterator<StackTraceElement> iter = new ArrayIterator<StackTraceElement>(e.getStackTrace());
		while (iter.hasNext()) {
			StackTraceElement t = iter.next();
			if (!t.getClassName().equals("acme.util.Util")) {
				out.print(prefix);
				out.println("    " + t);
				break;
			}
		}
		while (iter.hasNext()) {
			StackTraceElement t = iter.next();
			out.print(prefix);
			out.println("    " + t);
		}
	}

	public static void printStack() {
		printStack(Util.out);
	}

	public static void printAllStacks(int depth, PrintWriter out) {
		Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
		out.println("----------");			
		for (Entry<Thread,StackTraceElement[]> t : stacks.entrySet()) {
			Thread thread = t.getKey();
			StackTraceElement[] elems = t.getValue();
			out.printf("%-35s      state = %10s   depth = %5d \n", thread, thread.getState(), elems.length);
			int max = depth;
			for (StackTraceElement ste : elems) {
				out.println("    " + ste);
				if (max-- == 0) break;
			}
			out.println("----------");
		}
	}

	public static void printAllStacks(int depth) {
		printAllStacks(depth, Util.out);
	}

}
