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

package tools.atomizer;

import rr.error.ErrorMessage;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MetaDataInfo;
import rr.meta.MethodInfo;
import rr.state.ShadowThread;
import rr.tool.RR;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.NullDefault;

public final class AtomicBlock {

	protected static Decoration<MethodInfo,AtomizerErrorStatus> atomicStatus = MetaDataInfoMaps.getMethods().makeDecoration("atomicity status", DecorationFactory.Type.MULTIPLE, new NullDefault<MethodInfo,AtomizerErrorStatus>());


	MethodInfo data;
	final ShadowThread cur;
	final ErrorMessage<MethodInfo> errorMessage;

	public static final int MATCH_RIGHT = 0, MATCH_LEFT = 1, MATCH_NONE = 2;

	int phase;
	MetaDataInfo commitCause, errorCause;
	Throwable enterStack, commitStack, errorStack;


	public AtomicBlock(ShadowThread cur, ErrorMessage<MethodInfo> errorMessage) {
		this.cur = cur;
		this.errorMessage = errorMessage;
	}

	public void begin(final MethodInfo ad) {
		phase = MATCH_RIGHT;
		this.data = ad;
		if (RR.stackOption.get()) {
			enterStack = new Throwable();
		}
		commitStack = errorStack = null;
		commitCause = errorCause = null;
	}

	public void rightMover(MetaDataInfo fld) {
		if (phase == MATCH_LEFT) {	
			if (RR.stackOption.get()) {
				errorStack = new Throwable();
			}
			this.errorCause = fld; 
			errorMessage.error(cur, data,
					 "Enter Stack",	 acme.util.StackDump.stackDump(enterStack),
					 "Commit Cause", this.commitCause,
					 "Commit Stack", acme.util.StackDump.stackDump(commitStack),
					 "Error Cause",  this.errorCause,
					 "Error Stack",  acme.util.StackDump.stackDump(errorStack));
			updateStatus();
			phase = MATCH_NONE;
		}
	}

	public void leftMover(MetaDataInfo fld) {
		if (phase == MATCH_RIGHT) {
			if (RR.stackOption.get()) {
				commitStack = new Throwable();
			}
			phase = MATCH_LEFT;
			this.commitCause = fld; 
		}
	}

	public void nonMover(MetaDataInfo fld) {
		if (phase == MATCH_LEFT) {
			if (RR.stackOption.get()) {
				errorStack = new Throwable();
			}
			errorCause = fld;
			errorMessage.error(cur, data,
					 "Enter Stack",	 acme.util.StackDump.stackDump(enterStack),
					 "Commit Cause", this.commitCause,
					 "Commit Stack", acme.util.StackDump.stackDump(commitStack),
					 "Error Cause",  this.errorCause,
					 "Error Stack",  acme.util.StackDump.stackDump(errorStack));
			updateStatus();
			phase = MATCH_NONE;
		} else if (phase == MATCH_RIGHT) {
			if (RR.stackOption.get()) {
				commitStack = new Throwable();
			}
			commitCause = fld;
			phase = MATCH_LEFT;
		}
	}

	private void updateStatus() {
		if (atomicStatus.get(data) == null) {
			atomicStatus.set(data, new AtomizerErrorStatus(commitCause, errorCause));
		}
	}
}
