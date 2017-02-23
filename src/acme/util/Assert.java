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

 
/**
 * Routines to assert conditions are true, to fail, and to panic.  Panic is reserved for when the JVM will not be able to shutdown gracefully.
 */
public class Assert {


	static private int numWarnings = 0;
	static private boolean failed = false;
	static private String failedReason = "";
	
	public static void warn(String format, Object... args) {
		synchronized(Util.class) {
			//			Util.pad();
			Util.error("WARNING: " + format, args);
			Util.err.println();
			numWarnings++;
		}
	}
	
	public static void warn(Throwable e) {
		warn("%s", e);
	}

	public static void assertTrue(boolean b) {
		if (!b) {
			Assert.fail("Assertion Failure");
		}
	}

	public static void assertTrue(boolean b, String s) {
		if (!b) {
			Assert.fail("Assertion Failure: %s", s);
		}
	}

	public static void assertHoldsLock(Object l) {
		assertTrue(Thread.holdsLock(l));
	}

	public static void assertHoldsLock(Object l, String s, Object args) {
		Assert.assertTrue(Thread.holdsLock(l), s, args);
	}

	public static void fail(Throwable e) {
		Assert.fail(e.toString(), e);
	}

	public static void assertTrue(boolean b, String s, Object... args) {
		if (!b) {
			Assert.fail("Assertion Failure: " + s, args);
		}
	}

	public static void fail(String s, Object... args) {
		Assert.fail(String.format(s, args), new Throwable()); 
	}

	public static void fail(String s, Throwable e) {
		Assert.failed = true;
		Assert.failedReason = s + "(" + e + ")";
		synchronized(Util.class) {
			Util.error("\n");
			Util.error("%s ", s);
			Util.error("\n");
			StackDump.printStack(Util.err, e, Util.ERROR_PREFIX);
			Throwable cause = e.getCause();
			if (cause != null) {
				Util.error("Caused by...\n %s \n", cause.getMessage());
				StackDump.printStack(Util.err, cause, Util.ERROR_PREFIX);
			}
			Util.err.flush();
		}
		Util.exit(1);
	}

	public static void panic(String s) {
		Assert.failed = true;
		Assert.failedReason = s  + "(panicked)";

		Util.error("\n");
		Util.error("PANIC %s\n", s);
		StackDump.printStack(Util.err, new Throwable(), Util.ERROR_PREFIX);
		Util.error("\n");
		Runtime.getRuntime().halt(17);
	}

	public static void panic(String s, Object... args) {
		panic(String.format(s, args)); 
	}

	public static void panic(Throwable e) {
		Assert.failed = true;
		if (e instanceof OutOfMemoryError) {
			System.err.println("## Out of Memory");
			System.err.flush();
		} else {
			String exc = e.toString();
			Assert.failedReason = exc;
			Util.error("\n");
			Util.error("PANIC %s\n", exc);
			StackDump.printStack(Util.err, new Throwable(), Util.ERROR_PREFIX);
			Throwable cause = e;
			while (cause != null) {
				Util.error("Caused by [%s]...\n", cause.getClass());
				Util.error("%s\n", cause.getCause());
				Util.error("%s\n", cause.getMessage());
				StackDump.printStack(Util.err, cause, Util.ERROR_PREFIX);
				cause = cause.getCause();
			}
			Util.err.flush();
		}
		Runtime.getRuntime().halt(17);
	}

	public static boolean getFailed() {
		return Assert.failed;
	}
	
	public static String getFailedReason() {
		return Assert.failedReason;
	}

	/*****************/
	
	public static int getNumWarnings() {
		return Assert.numWarnings;
	}


}
