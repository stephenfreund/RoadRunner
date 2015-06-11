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

import java.lang.reflect.Field;

import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;

import rr.instrument.Constants;
import rr.loader.LoaderContext;
import rr.state.ShadowVar;
import acme.util.Assert;
import acme.util.count.Counter;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

// Since Clone copies instrumentation fields, we need to wipe them...

public class CloneFixer extends RRClassAdapter {

	public static final CommandLineOption<Boolean> noCloneOption = CommandLine.makeBoolean("noClone", false, CommandLineOption.Kind.STABLE, "turn off special handling of clone");

	private class CloneMethodVisitor extends MethodVisitor {

		public CloneMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			if (!noCloneOption.get()) {
				if (name.equals("clone") && desc.equals("()Ljava/lang/Object;")) {
					try {
						LoaderContext.bootLoaderContext.getRRClass("rr/instrument/classes/CloneFixer");
					} catch (ClassNotFoundException e) {
						Assert.panic(e);
					}
					super.visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/CloneFixer", "__$rr_fixClone", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
				} 
			}
		}
	}

	public CloneFixer(ClassVisitor cv) {
		super(cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new CloneMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions));
	}

	private static Counter cloneCount = new Counter("Clone", "Count");

	public static final Object __$rr_fixClone(Object o) {
		Class<?> c = o.getClass();
		for (Field f : c.getFields()) {
			String name = f.getName();
			if (Constants.isSyntheticName(name) && f.getType() == rr.state.ShadowVar.class && ((f.getModifiers() & Opcodes.ACC_STATIC) == 0)) {
				try {
					final ShadowVar shadowVar = (ShadowVar)f.get(o);
					ShadowVar v = rr.tool.RREventGenerator.cloneVariableState(shadowVar);
				//	Util.logf("Clone %s.%s : tools.internal=%s new=%s", Util.objectToIdentityString(o), f, shadowVar, v);
					f.set(o, v);
				} catch (Exception e) {
					Assert.panic(e);
				}
			}
		}
		cloneCount.inc();
		return o;
	}

}
