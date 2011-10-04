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

package tools.eraser;
import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.event.VolatileAccessEvent;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import acme.util.option.CommandLine;


@Abbrev("RS")
final public class ReadSharedTool extends Tool {

	public static class ReadShared implements ShadowVar {
		
		private static final ReadShared single = new ReadShared();
		
		private ReadShared() {
		}
		
		public static ReadShared get() {
			return single;
		}
		
		@Override
		public String toString() {
			return "READ SHARED";
		}
	}

	
	public ReadSharedTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) {
		super.volatileAccess(fae);
	}

	@Override
	public void access(AccessEvent aae) {
		if (aae.getOriginalShadow() == ReadShared.get()) {
			if (aae.isWrite()) {
				advance(aae);
			} 
		} else {
			super.access(aae);
		} 
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		if (fae.isWrite()) {
			return super.makeShadowVar(fae);
		} else {
			return ReadShared.get();
		}
	}


	public static boolean readFastPath(ShadowVar vs, ShadowThread ts) {
		return vs == ReadShared.get();
	}
}
