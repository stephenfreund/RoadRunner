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

import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.Method;

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
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		final ClassInfo owner = this.getCurrentClass();
		if ((access & Opcodes.ACC_ABSTRACT) != 0) {
			Util.log("Fixing Potential Orphan " + MetaDataInfoKeys.getMethodKey(owner, name, desc));
			MethodVisitor mv = super.visitMethod(access & ~Opcodes.ACC_ABSTRACT, name, desc, signature, exceptions);
			Type type = Type.getReturnType(desc);
			mv.visitLdcInsn("Dispatched abstract method " + owner + "." + name + ":" + desc + " -- did you not instrument a subclass of instrumented class???");
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "acme/util/Yikes", "yikes", Method.getMethod("boolean yikes(Object)").getDescriptor(), false);
			mv.visitInsn(Opcodes.POP);
			mv.visitVarInsn(Opcodes.ALOAD, 0);
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
				mv.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
				localVarIndex += args[i].getSize();
				args2[i] = args[i];
			} 

			name = Constants.recoverOriginalNameFromMangled(name);
			mv.visitMaxs(10,  10);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, owner.getName(), name, Type.getMethodDescriptor(Type.getReturnType(desc), args2), false);
			mv.visitInsn(ASMUtil.returnInstr(type));
			mv.visitEnd();
			return mv;
		} else {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}


}
