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

package rr.instrument.tools;

import java.util.Vector;

import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.tool.Tool;
import acme.util.StringMatchResult;
import acme.util.StringMatcher;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

@Abbrev("AFT")
public class ArrayFilterTool extends Tool {

	public final static CommandLineOption<StringMatcher> arrayAllocsToWatch  = 
		CommandLine.makeStringMatcher("arrayAllocs", StringMatchResult.ACCEPT, CommandLineOption.Kind.EXPERIMENTAL, "Specifies which array alloc sites to watch.  The default is all");

	public ArrayFilterTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
		commandLine.add(arrayAllocsToWatch);
	}

	private Vector<Object> ok = new Vector<Object>();
	private Vector<Object> bad = new Vector<Object>();

	boolean ok(Object o) {
		boolean b =ArrayAllocSiteTracker.get(o) != null;
		return b;
	}

	@Override
	public void access(AccessEvent fae) {
		if (fae.getKind() != AccessEvent.Kind.ARRAY || ok(fae.getTarget())) {
			super.access(fae);
		} 
	}


}
