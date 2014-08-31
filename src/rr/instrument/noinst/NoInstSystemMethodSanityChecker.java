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

package rr.instrument.noinst;

import rr.org.objectweb.asm.MethodVisitor;

import rr.instrument.methods.Replacement;
import rr.instrument.methods.SystemMethodReplacer;
import rr.loader.MethodResolutionException;
import rr.loader.RRTypeInfo;
import rr.meta.ClassInfo;
import rr.meta.MethodInfo;
import acme.util.Assert;

public class NoInstSystemMethodSanityChecker extends SystemMethodReplacer {

	public NoInstSystemMethodSanityChecker(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		try {
			MethodInfo m = RRTypeInfo.resolveMethodDescriptor(owner, name, desc);

			if (!m.isSynthetic()) {
				if (!owner.startsWith("[")) {
					for (ClassInfo c : RRTypeInfo.declaringClassesForMethodDescriptor(RRTypeInfo.resolveMethodDescriptor(owner, name, desc))) {
						for (Replacement r : systemReplacements) {
							if (r.matches(opcode, c.getName(), name, desc)) {
								Assert.warn("Non-instrumented method " + this.getMethod() + " has call to " + owner + "." + name);
							}
						}
					}
				}
			}
		} catch (MethodResolutionException e) {
			Assert.warn("Can't find method in NoInst System Sanity Checker: " + e);
		}
		super.visitMethodInsn(opcode, owner, name, desc, isInterface);
	}
}
