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

package rr.instrument.classes;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.meta.ClassInfo;
import rr.meta.MetaDataInfoKeys;
import acme.util.Util;

public class AbstractOrphanFixer extends RRClassAdapter {

	public AbstractOrphanFixer(ClassVisitor cv) {
		super(cv);
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
		final ClassInfo owner = this.getCurrentClass();
		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			class AbstractOrphanFixerMethodVisitor extends MethodAdapter {
				private boolean hasCode = false;
				public AbstractOrphanFixerMethodVisitor(MethodVisitor mv) {
					super(mv);
				}
				@Override
				public void visitCode() {
					hasCode = true;
					super.visitCode();
					Util.log("Fixing Potential Orphan " + MetaDataInfoKeys.getMethodKey(owner, name, desc));
					Type type = Type.getReturnType(desc);
					super.visitLdcInsn("Dispatched abstract method " + owner + "." + name + ":" + desc + " -- did you not instrument a subclass of instrumented class???");
					super.visitMethodInsn(Opcodes.INVOKESTATIC, "acme/util/Yikes", "yikes", Method.getMethod("boolean yikes(Object)").getDescriptor());
					super.visitVarInsn(Opcodes.ALOAD, 0);
					Type args[] = Type.getArgumentTypes(desc);
					int n;
					if (args.length > 0 && args[args.length - 1].equals(Constants.THREAD_STATE_TYPE)) {
						n = args.length - 1;
					} else {
						n = args.length;
					}
					Type args2[] = new Type[n];
					int localVarIndex = 1;
					for (int i = 0; i < n; i++) {
						super.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
						localVarIndex += args[i].getSize();
						args2[i] = args[i];
					} 

					super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner.getName(), Constants.recoverOriginalNameFromMangled(name), Type.getMethodDescriptor(Type.getReturnType(desc), args2));
					super.visitInsn(ASMUtil.returnInstr(type));
					
				}
				
				private int maxStack = 0, maxLocal = 0;
				@Override
				public void visitMaxs(int maxStack, int maxLocal) {
					this.maxStack = maxStack;
					this.maxLocal = maxLocal;
				}
				
				@Override
				public void visitEnd() {
					if (!hasCode) {
						visitCode();
					}
					super.visitMaxs(maxStack + 10, maxLocal + 10);
					super.visitEnd();
				}
			}

			return new AbstractOrphanFixerMethodVisitor(super.visitMethod(access & ~Opcodes.ACC_ABSTRACT, name, desc, signature, exceptions));
		} else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}


}
