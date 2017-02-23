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

package rr.loader;

import java.util.Stack;

import rr.instrument.Instrumentor;
import rr.meta.ClassInfo;
import rr.meta.FieldInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.meta.ClassInfo.State;
import rr.org.objectweb.asm.ClassReader;
import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.FieldVisitor;
import rr.org.objectweb.asm.Label;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import acme.util.Assert;
import acme.util.Yikes;

public class MetaDataBuilder {

	static private Stack<String> preLoad = new Stack<String>();

	private static class MetaDataClassVisitor extends ClassVisitor {

		protected final boolean sigsOnly;
		protected ClassInfo current;
		protected LoaderContext ctxt;

		public MetaDataClassVisitor(LoaderContext c, boolean sigsOnly) {
			super(Opcodes.ASM5);
			this.sigsOnly = sigsOnly;
			this.ctxt = c;
		}

		@Override
		public void visit(int version, int access, String name,
				String signature, String superName, String[] interfaces) {

			current = MetaDataInfoMaps.getClass(name);

			if ((access & Opcodes.ACC_INTERFACE) == 0 && superName != null) {
				try {
					ctxt.getRRClass(superName);
				} catch (ClassNotFoundException e) {
					Assert.fail(e);
				}
				preLoadRec(superName);
				MetaDataInfoMaps.getClass(superName);
				if (current.stateAtMost(ClassInfo.State.IN_PRELOAD)) {
					current.setSuperClass(MetaDataInfoMaps.getClass(superName));
				}
			}
			for (String i : interfaces) {
				preLoadRec(i);
				MetaDataInfoMaps.getClass(i);
				if (current.stateAtMost(ClassInfo.State.IN_PRELOAD)) {
					current.addInterface(MetaDataInfoMaps.getClass(i));
				}
			}
			super.visit(version, access, name, signature, superName, interfaces);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			if (sigsOnly) {
				current.setState(ClassInfo.State.PRELOADED);
			} else {
				current.setState(ClassInfo.State.COMPLETE);
				Loader.notify(current);
			}
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc,
				String signature, Object value) {
			Type t = Type.getType(desc);
			visitType(t); 
			FieldInfo field = MetaDataInfoMaps.getField(current, name, desc);
			if (current.stateAtMost(ClassInfo.State.IN_PRELOAD)) {
				field.setFlags((access & Opcodes.ACC_FINAL) != 0, (access & Opcodes.ACC_VOLATILE) != 0, (access & Opcodes.ACC_STATIC) != 0);
			}

			return super.visitField(access, name, desc, signature, value);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			visitType(Type.getReturnType(desc));
			for (Type t : Type.getArgumentTypes(desc)) {
				visitType(t);
			}
			MethodInfo method = MetaDataInfoMaps.getMethod(current, name, desc);
			method.setFlags((access & Opcodes.ACC_STATIC) != 0,(access & Opcodes.ACC_NATIVE) != 0, (access & Opcodes.ACC_SYNCHRONIZED) != 0);
			final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return sigsOnly ? mv : new MetaDataMethodVisitor(method, mv);
		}

	}

	private static class MetaDataMethodVisitor extends MethodVisitor {

		protected final MethodInfo method;

		public MetaDataMethodVisitor(MethodInfo method, MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
			this.method = method;
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			super.visitMaxs(maxStack, maxLocals);
			Instrumentor.methodContext.get(method).setFirstFreeVar(maxLocals);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name,
				String desc) {
			preLoadRec(owner);
			visitType(Type.getType(desc));
			super.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc, boolean itf) {
			preLoadRec(owner);
			visitType(Type.getReturnType(desc));
			for (Type t : Type.getArgumentTypes(desc)) {
				visitType(t);
			}
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			visitType(Type.getType(desc));
			super.visitMultiANewArrayInsn(desc, dims);
		}

		@Override
		public void visitTypeInsn(int opcode, String desc) {
			visitType(Type.getObjectType(desc));
			super.visitTypeInsn(opcode, desc);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler,
				String type) {
			if (type != null) {
				visitType(Type.getObjectType(type));
			}
			super.visitTryCatchBlock(start, end, handler, type);
		}

	}

	public static void preLoad(LoaderContext c, ClassReader in) {
		MetaDataClassVisitor mcv = new MetaDataClassVisitor(c, true);
		in.accept(mcv, 0);
	}

	public static void preLoadFully(final LoaderContext c, final byte b[])  {
		preLoadFully(c, new ClassReader(b));
	}

	public static void preLoadFully(final LoaderContext c, final ClassReader in)  {
		MetaDataClassVisitor mcv = new MetaDataClassVisitor(c, false);
		in.accept(mcv, 0);

		while (!preLoad.isEmpty()) {
			final String pop = preLoad.pop();
			try {
				ClassInfo r = c.getRRClass(pop);
			} catch (ClassNotFoundException e) {
				Yikes.yikes("Failed to load class " + pop + ".  Hopefully just because RR is more eager in loading than JVM...");
				MetaDataInfoMaps.getClass(pop).setState(State.COMPLETE);
			}
		}
	}

	public static void visitType(Type t) {
		switch (t.getSort()) {
			case Type.ARRAY:
				visitType(t.getElementType());
				break;
			case Type.OBJECT:
				preLoadRec(t.getInternalName());
				break;
		}

	}

	public static ClassInfo preLoad(String className) {
		return MetaDataInfoMaps.getClass(className);
	}


	private static void preLoadRec(String className) {
		if (!preLoad.contains(className)) {
			preLoad.push(className);
		}
	}

}
