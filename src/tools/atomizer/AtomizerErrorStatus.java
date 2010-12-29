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

import rr.meta.FieldAccessInfo;
import rr.meta.MetaDataInfo;

public class AtomizerErrorStatus {

	public final MetaDataInfo commit, error;

	public AtomizerErrorStatus(MetaDataInfo commit, MetaDataInfo error) {
		this.commit = commit;
		this.error = error;
	}
	@Override
	public String toString() {
		boolean commitRace = commit instanceof FieldAccessInfo;
		boolean errorRace = error instanceof FieldAccessInfo;
		String message = "???";
		if (commitRace && errorRace) { 
			message = "RaceRace";
		} else if (!commitRace && errorRace) {
			message = "ReleaseRace";
		} else if (commitRace && !errorRace) {
			message = "RaceAcquire";
		} else {
			message = "AcquireRelease";
		}
		return String.format("%s <commit>%s</commit> <error>%s</error>", message, commit, error, message);
	}
}
