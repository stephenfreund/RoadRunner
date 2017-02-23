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

 ***********************************************************************/

package acme.util;

import java.util.HashMap;
import java.util.Map.Entry;

import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Routines to print "yikes" messages -- unusual internal circumstances 
 * that are worth noting but don't cause failure. 
 */
public class Yikes {

	public static final CommandLineOption<Integer> maxYikesOption = 
			CommandLine.makeInteger("maxYikes", 5, CommandLineOption.Kind.STABLE, "Maximum number of each yikes message printed.");

	private static int numYikes = 0;
	private static HashMap<String, Integer> yikesMessages = new HashMap<String,Integer>();

	public static boolean yikes(String format, Object... args) {
		synchronized(Util.class) {
			String msg = String.format(format, args);
			Integer n = yikesMessages.get(msg);
			if (n == null) {
				n = 1;
			} else {
				n++;
			}
			numYikes++;
			yikesMessages.put(msg, n);
			if (n <= Yikes.maxYikesOption.get()) {
//				Util.pad();
				Util.error("YIKES: " + msg);
				if (n== Yikes.maxYikesOption.get()) {
					Util.error("Suppressing further yikes messages like that one."); 
				}
				Util.err.println();	 
				return true;
			} else {
				return false;
			}
		}
	}

	public static boolean yikes(Throwable e) {
		return Yikes.yikes(e.toString(), e);
	}

	public static boolean yikes(String s, Throwable e) {
		synchronized(Util.class) {
			boolean b = yikes("%s", s);
			if (b) {
				Util.error("\n");
				StackDump.printStack(Util.err, e, Util.ERROR_PREFIX);
				Throwable cause = e.getCause();
				if (cause != null) {
					Util.error("Caused by...\n %s \n", cause.getMessage());
					StackDump.printStack(Util.err, cause, Util.ERROR_PREFIX);
				}
				Util.err.flush();
			}
			return b;
		}
	}

	public static boolean yikes(Object o) {
		return yikes("%s", o.toString());
	}

	public static int getNumYikes() {
		return numYikes;
	}

	public static int getNumYikes(String s) {
		synchronized(Util.class) {
			if (yikesMessages.containsKey(s)) {
				return yikesMessages.get(s);
			} else {
				return 0;
			}
		}
	}
	
	public static synchronized void printXML(XMLWriter out) {
		out.push("yikes");
		for (Entry<String,Integer> e : yikesMessages.entrySet()) {
			String key = e.getKey();
			out.printWithFixedWidths("message", key.substring(0,Math.min(key.length(), 35)), -35, "count", e.getValue(), -15);
		}
		out.pop();
	}
}
