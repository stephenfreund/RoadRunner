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

package rr.state.update;

import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public class Updaters {

	public static enum UpdateMode { SAFE, UNSAFE, CAS };

	public static CommandLineOption<UpdateMode> updateOptions = 
			CommandLine.makeEnumChoice("updaters", UpdateMode.SAFE, CommandLineOption.Kind.EXPERIMENTAL, "Specify whether to use synchronized (safe) or unsynchronized (unsafe) updates to shadow locations.  You should leave this as SAFE unless there is a compelling argument why it is not needed. Unsynchronized are faster may cause subtle issues because of the JMM. CAS is EXPERIMENTAL --- use at your own risk (see CASFieldUpdater.java)", UpdateMode.class);

	public static Class<? extends UnsafeFieldUpdater> fieldUpdaterClass() {
		return (updateOptions.get() == UpdateMode.SAFE) ? SafeFieldUpdater.class : UnsafeFieldUpdater.class;
	}

	public static Class<? extends AbstractArrayUpdater> arrayUpdaterClass() {
		switch (updateOptions.get()) {
			case SAFE : return SafeArrayUpdater.class;
			case UNSAFE: return UnsafeArrayUpdater.class;
			case CAS : return CASArrayUpdater.class;
			default: return null;
		}
	}

}
