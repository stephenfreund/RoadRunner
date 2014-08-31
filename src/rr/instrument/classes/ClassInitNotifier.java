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

import rr.loader.LoaderContext;
import rr.meta.ClassInfo;
import acme.util.Assert;

// Since Clone copies instrumentation fields, we need to wipe them...

public class ClassInitNotifier extends RRClassAdapter {


	private ClassInfo currentClass;

	private class ClassInitVisitor extends MethodVisitor {

		public ClassInitVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.RETURN) {
				try {
					LoaderContext.bootLoaderContext.getRRClass("rr/instrument/classes/ClassInitNotifier");
				} catch (ClassNotFoundException e) {
					Assert.panic(e);
				}
				super.visitLdcInsn(currentClass.getKey());
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/ClassInitNotifier", "__$rr_init", "(Ljava/lang/String;)V", false);
			}
			super.visitInsn(opcode);
		} 

	}


	public ClassInitNotifier(ClassInfo currentClass, ClassVisitor cv) {
		super(cv);
		this.currentClass = currentClass;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!name.equals("<clinit>")) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		} else {
			return new ClassInitVisitor(super.visitMethod(access, name, desc, signature, exceptions));
		}
	}

	public static final void __$rr_init(String className) {
		try {
			rr.tool.RREventGenerator.classInitEvent(className);
		} catch (Exception e) {
			Assert.panic(e);
		}
	}

}
