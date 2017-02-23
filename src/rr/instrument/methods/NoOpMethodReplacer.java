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

package rr.instrument.methods;

import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;

import rr.loader.RRTypeInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MethodInfo;
import acme.util.Assert;
import acme.util.Util;

public class NoOpMethodReplacer extends RRMethodAdapter implements Opcodes {

	public NoOpMethodReplacer(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		// SNF: Changed to catch exceptions, since this is trying to find methods 
		//   in a way that is currently not working.
		try {
			MethodInfo m = RRTypeInfo.resolveMethodDescriptor(owner, name, desc);
			if (InstrumentationFilter.isNoOp(m)) {
				Assert.assertTrue(Type.getReturnType(desc) == Type.VOID_TYPE, "Can only turn void methods into no ops");
				Type[] t = Type.getArgumentTypes(desc);
				for (int i = 0; i < t.length; i++) {
					this.pop();
				}
				if (opcode != Opcodes.INVOKESTATIC) {
					this.pop();
				}
			} else {
				super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			}
		} catch(Exception e) {
			Util.log("Can't find method in NoOp Method Replacer: " + e);
			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		}
	}
}
