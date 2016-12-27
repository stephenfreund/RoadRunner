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
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.ArrayAccessEvent;
import rr.event.FieldAccessEvent;
import rr.event.NewThreadEvent;
import rr.event.ReleaseEvent;
import rr.event.VolatileAccessEvent;
import rr.event.AccessEvent.Kind;
import rr.meta.ArrayAccessInfo;
import rr.meta.FieldInfo;
import rr.simple.LastTool;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import tools.util.LockSet;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;

@Abbrev("LS")
final public class LockSetTool extends Tool {

	public final ErrorMessage<FieldInfo> fieldErrors = 
		ErrorMessages.makeFieldErrorMessage("LockSet");

	public final ErrorMessage<ArrayAccessInfo> arrayErrors = 
		ErrorMessages.makeArrayErrorMessage("LockSet");

	public LockSetTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);

		if (!(next instanceof LastTool)) {
			fieldErrors.setMax(1);
			arrayErrors.setMax(1);
		}
	}

	protected static LockSet ts_get_lset(ShadowThread td) { Assert.fail("bad"); return null;}
	protected static void ts_set_lset(ShadowThread td, LockSet ls) { Assert.fail("bad");  }

	public static LockSet getLockSetForThread(ShadowThread state) {
		return ts_get_lset(state);
	}

	public static void setLockSetForThread(ShadowThread state, LockSet ls) {
		ts_set_lset(state, ls);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) { 
		super.volatileAccess(fae);
	}

	@Override
	public void access(AccessEvent fae) {

		ShadowThread currentThread = fae.getThread();

		while (true) {
			ShadowVar g = fae.getOriginalShadow();

			if (g instanceof LockSet) {

				LockSet ls = (LockSet) g;

				LockSet ls2 = LockSet.intersect(ls, ts_get_lset(currentThread));

				if (ls != ls2) {
					if (!fae.putShadow(ls2)) {
						Yikes.yikes("concurrent update in LockSet");
						continue;
					}
				}

				if (ls2.isEmpty()) {
					error(fae, g);
				}
				return;
			}
			super.access(fae);
			return;
		}
	}

	private void error(AccessEvent fae, ShadowVar g) {
		ShadowThread currentThread = fae.getThread();
		if (fae.getKind() != Kind.ARRAY) {
			FieldInfo fd = ((FieldAccessEvent)fae).getInfo().getField();

			if (fieldErrors.stillLooking(fd)) {
				fieldErrors.error(currentThread, 
						fd, 
						"Guard State",	g,
						"Class", 		fd.getOwner(),
						"Field", 		fd.getName(),
						"Target", 		Util.objectToIdentityString(fae.getTarget()),
						"Locks", 		ts_get_lset(currentThread),
						"Stack Trace",	ShadowThread.stackDumpForErrorMessage(currentThread));
			}

			if (!fieldErrors.stillLooking(fd)) advance(fae);
		} else {
			ArrayAccessInfo fd = ((ArrayAccessEvent)fae).getInfo();

			if (arrayErrors.stillLooking(fd)) {
				arrayErrors.error(currentThread, 
						fd, 
						"Guard State", 					g, 
						"Array", 						Util.objectToIdentityString(fae.getTarget()), 
						"Index", 						fae.getTarget(),
						"Locks", 						ts_get_lset(currentThread),
						"Stack Trace",					ShadowThread.stackDumpForErrorMessage(currentThread));
			}

			if (!arrayErrors.stillLooking(fd)) advance(fae);
		}
	}

	@Override
	public void acquire(AcquireEvent ae) {
		ShadowThread currentThread = ae.getThread();
		ts_set_lset(currentThread, ts_get_lset(currentThread).add(ae.getLock()));
		super.acquire(ae);
	}

	@Override
	public void release(ReleaseEvent re) {
		ShadowThread currentThread = re.getThread();
		ts_set_lset(currentThread, ts_get_lset(currentThread).remove(re.getLock()));
		super.release(re);
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		if (fae.getKind() != Kind.VOLATILE) {
			return ts_get_lset(fae.getThread());
		} else {
			return super.makeShadowVar(fae);	
		}
	}

	@Override
	public void printXML(XMLWriter xml) {
		int a[] = LockSet.cacheSizes();
		int total = 0;
		for (int i = 0; i < a.length; i++) {
			total += i * a[i];
		}		
		xml.print("lscache", total);
	}

	@Override
	public void create(NewThreadEvent e) {
		ShadowThread td = e.getThread();
		ts_set_lset(td, LockSet.emptySet());
		super.create(e);

	}

	public static boolean readFastPath(final ShadowVar g, final ShadowThread currentThread) {
		final LockSet lset = ts_get_lset(currentThread);
		return (g == lset && !lset.isEmpty());
	}

	public static boolean writeFastPath(final ShadowVar g, final ShadowThread currentThread) {
		final LockSet lset = ts_get_lset(currentThread);
		return (g == lset && !lset.isEmpty());
	}

}
