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

package rr.state.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import rr.org.objectweb.asm.ClassReader;
import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.ClassWriter;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;

import rr.loader.Loader;
import acme.util.Assert;
import acme.util.Util;

public class StateExtensionTransformer implements ClassFileTransformer {

	private DefineClassListener hook;
	
	public byte[] transform(ClassLoader definingLoader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] bytes) throws IllegalClassFormatException {

		if (!(className.startsWith("rr/") || className.startsWith("tools/") || className.startsWith("java/") || className.startsWith("acme/") || className.startsWith("sun/"))) {
			if (hook != null) {
				return hook.define(definingLoader, className, bytes);
			}
		}

		if (ThreadStateExtensionAgent.noDecorationInline.get()) {
			return null;
		}

		if (className.equals("rr/state/ShadowThread")) {
			return transformThreadState(definingLoader, className,
					classBeingRedefined, protectionDomain,
					bytes);
		} else if (classesToTransform.contains(className)) {
			return transformHelper(definingLoader, className,
					classBeingRedefined, protectionDomain,
					bytes);
		} else {
			return null;
		}
	}

	final Vector<ThreadStateFieldExtension> fields = new Vector<ThreadStateFieldExtension>();
	final Set<String> classesToTransform = new HashSet<String>();

	public void addField(ThreadStateFieldExtension f) {
		for (ThreadStateFieldExtension o : fields) {
			if (o.name.equals(f.name)) { 
				Assert.warn("Potential ThreadState field extension name clash: '%s'", o.name);
			}
		}
		fields.add(f);
	}

	private class ThreadStateClassVisitor extends ClassVisitor implements Opcodes {

		final String className;

		public ThreadStateClassVisitor(ClassVisitor cv, String className) {
			super(ASM5, cv);
			this.className = className;
		}

		@Override
		public void visitEnd() {
			for (ThreadStateFieldExtension f : fields) {
				if (f.owner.equals(className)) {
					Util.log("Adding field " + f.name + " to " + f.owner + " (" + f.origin + ")");
					this.visitField(Opcodes.ACC_PUBLIC, f.name + "_" + f.origin.replace("/", "_"), f.desc, null, null);					
				}
			}
			super.visitEnd();
		}		
	}


	private byte[] transformThreadState(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {
		Util.log("Transforming " + className);
		try {
			ClassReader cr = new ClassReader(classfileBuffer); 
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor cv = new ThreadStateClassVisitor(cw, className);
			cr.accept(cv, 0);
			byte b[] = cw.toByteArray();
			Loader.writeToFileCache("transformed", className, b);
			Util.log("done");
			return b;
		} catch (Exception e) {
			Assert.panic(e);
			return null;
		}
	}


	public void addToolClassToWatchList(String name) {
		classesToTransform.add(name);
	}

	private class WatchedClassMethodVisitor extends MethodVisitor implements Opcodes {

		String className;

		public WatchedClassMethodVisitor(MethodVisitor mv, String className) {
			super(ASM5, mv);
			this.className = className;
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
			//if (owner.equals(className)) {  Conflicts are now the user's problem...
				for (ThreadStateFieldExtension f : fields) {
				//	if (f.origin.equals(className)) {
						if (name.equals("ts_get_" + f.name)) {
							super.visitFieldInsn(GETFIELD, f.owner, f.name + "_" + f.origin.replace("/", "_"), f.desc);
							return;
						} else if (name.equals("ts_set_" + f.name)) {
							super.visitFieldInsn(PUTFIELD, f.owner, f.name + "_" + f.origin.replace("/", "_"), f.desc);
							return;
						} 
				//	}
				}
			//}
			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		}
	}

	private class WatchedClassVisitor extends ClassVisitor implements Opcodes {

		final String className;

		public WatchedClassVisitor(ClassVisitor cv, String className) {
			super(ASM5, cv);
			this.className = className;
		}


		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			return new WatchedClassMethodVisitor(super.visitMethod(access, name, desc, signature, exceptions), className);
		}

		@Override
		public void visitEnd() {
			for (ThreadStateFieldExtension f : fields) {
				if (f.owner.equals(className)) {
					Util.log("Adding field " + f.name + " to " + f.owner  + " (" + f.origin + ")");
					this.visitField(Opcodes.ACC_PUBLIC, f.name + "_" + f.origin.replace("/", "_"), f.desc, null, null);					
				}
			}
			super.visitEnd();
		}		
	}

	private byte[] transformHelper(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {
		Util.log("Transforming " + className);
		try {
			ClassReader cr = new ClassReader(classfileBuffer); 
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			ClassVisitor cv = new WatchedClassVisitor(cw, className);
			cr.accept(cv, 0);
			byte b[] = cw.toByteArray();
			Loader.writeToFileCache("transformed", className, b);
			Util.log("done");
			return b;
		} catch (Exception e) {
			Assert.panic(e); 
			return null;
		}
	}

	public void setDefineClassHook(DefineClassListener hook2) {
		hook = hook2;
	}

}

