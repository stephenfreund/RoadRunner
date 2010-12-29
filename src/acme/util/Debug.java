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

import java.util.ArrayList;

import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Various routines to print debugging messages, depending on whether a key is enable with the "-d" command line option.  
 * So, if you run a program with option
 * <pre>
 * -d=moo
 * </pre>
 * the routine
 * <pre>
 * Debug.debug("moo", "hello");
 * </pre> 
 * will print that message.  WIthout the option, it is a no-op.
 *
 */
public class Debug {

	public static final CommandLineOption<ArrayList<String>> debugKeysOption = 
		CommandLine.makeStringList("d", CommandLineOption.Kind.STABLE, "Turn on the given debugging key.  Messages printed by Util.debugf(key, ...) will only be printed if the key is turned on.");
	
	
	public static void debug(String key, String s) {
		if (debugKeysOption.get().contains(key)) {
			Util.log(key + "-- " + s);
		}
	}

	public static void debugf(String key, String format, Object... args) {
		if (debugKeysOption.get().contains(key)) {
			Util.logf(key + "-- " + format, args);
		}
	}

	public static void debug(String key, boolean guard, String s) {
		if (guard && debugKeysOption.get().contains(key)) {
			Util.log(key + "-- " + s);  
		}
	}

	public static boolean debugOn(String key) {
		return debugKeysOption.get().contains(key);
	}

	public static void debug(String key, Runnable op) {
		if (debugKeysOption.get().contains(key)) {
			op.run();
		}
	}

}
