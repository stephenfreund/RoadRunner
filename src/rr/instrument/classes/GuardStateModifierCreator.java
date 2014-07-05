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

import rr.org.objectweb.asm.ClassWriter;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;

import rr.instrument.Constants;
import rr.loader.Loader;
import rr.state.update.Updaters;

public class GuardStateModifierCreator implements Opcodes {

	public static byte[] dump (String className, String fieldName, boolean isStatic, boolean isVolatile)  {

		ClassWriter cw = new ClassWriter(0);
		MethodVisitor mv;
		String thunkSuper = Updaters.fieldUpdaterClass().getCanonicalName().replace('.', '/');
		String thunkName = Constants.getUpdateThunkName(className, fieldName);
		thunkName = thunkName.replace('.', '/');
		cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, thunkName, null, thunkSuper, null);
		{
			mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, thunkSuper, "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		String gsName = Constants.getShadowFieldName(className, fieldName, isStatic, isVolatile);
		{
			mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)Lrr/state/ShadowVar;", null, null);
			mv.visitCode();
			if (!isStatic) {
				mv.visitVarInsn(ALOAD, 1);
				mv.visitTypeInsn(CHECKCAST, className);
				mv.visitFieldInsn(GETFIELD, className, gsName, Constants.GUARD_STATE_TYPE.getDescriptor());
			} else {
				mv.visitFieldInsn(GETSTATIC, className, gsName, Constants.GUARD_STATE_TYPE.getDescriptor());
			}
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 3);
			mv.visitEnd();
		}

		{
			mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;Lrr/state/ShadowVar;)V", null, null);
			mv.visitCode();
			if (!isStatic) {
				mv.visitVarInsn(ALOAD, 1);
				mv.visitTypeInsn(CHECKCAST, className);
				mv.visitVarInsn(ALOAD, 2);
				mv.visitFieldInsn(PUTFIELD, className, gsName, Constants.GUARD_STATE_TYPE.getDescriptor());
			} else {
				mv.visitVarInsn(ALOAD, 2);
				mv.visitFieldInsn(PUTSTATIC, className, gsName, Constants.GUARD_STATE_TYPE.getDescriptor());
			}
			mv.visitInsn(RETURN);
			mv.visitMaxs(3, 4);
			mv.visitEnd();
		}

		{
			mv = cw.visitMethod(ACC_PUBLIC, "hashCode", "()I", null, null);
			mv.visitCode();
			if (!isStatic) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "identityHashCode", "(Ljava/lang/Object;)I", false);
			} else {
				mv.visitLdcInsn(thunkName.hashCode());
			}
			mv.visitInsn(IRETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}


		cw.visitEnd();

		final byte[] byteArray = cw.toByteArray();
		Loader.writeToFileCache("updaters", thunkName, byteArray);
		return byteArray;
	}
}
