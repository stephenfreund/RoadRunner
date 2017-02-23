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

import java.util.Vector;

import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.Method;
import rr.RRMain;
import rr.instrument.Constants;
import rr.loader.LoaderContext;
import rr.loader.MethodResolutionException;
import rr.loader.RRTypeInfo;
import rr.meta.ClassInfo;
import rr.meta.InterruptInfo;
import rr.meta.JoinInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.meta.SourceLocation;
import rr.meta.StartInfo;
import rr.meta.WaitInfo;
import acme.util.Assert;
import acme.util.Util;

/*
 * System Methods that are replace should be called within the replacement, just in case
 * the invocation is actually going to dispatch an overridden version.  The exception
 * is if the system method is final.  System methods can also only be in package java/lang.
 */
public class SystemMethodReplacer extends RRMethodAdapter implements Opcodes {

	public SystemMethodReplacer(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
		try {
			LoaderContext.bootLoaderContext.getRRClass("rr/instrument/java/lang/System");
		} catch (ClassNotFoundException e) {
			Assert.panic(e);
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {
		try {
			MethodInfo m = RRTypeInfo.resolveMethodDescriptor(owner, name, desc);

			if (!m.isSynthetic()) {
				if (!owner.startsWith("[") && !name.startsWith("<")) {
					for (ClassInfo c : RRTypeInfo.declaringClassesForMethodDescriptor(RRTypeInfo.resolveMethodDescriptor(owner, name, desc))) {
						if (c.getName().startsWith("java/lang")) {
							for (Replacement r : systemReplacements) {
								if (r.matches(opcode, c.getName(), name, desc)) {
									if (RRMain.slowMode()) Util.logf("Replace %s.%s%s", c.getName(), name, desc);
									r.replace(opcode, this);
									return;
								}
							}
						}
					}
				}
			}
		} catch (MethodResolutionException e) {
			Util.log("Can't find method in System Method Replacer: " + e);
		}
		super.visitMethodInsn(opcode, owner, name, desc, isInterface);
	}

	protected static Vector<Integer> allOpcodes = new Vector<Integer>();
	static {
		allOpcodes.add(Opcodes.INVOKEINTERFACE);
		allOpcodes.add(Opcodes.INVOKESPECIAL);
		allOpcodes.add(Opcodes.INVOKESTATIC);
		allOpcodes.add(Opcodes.INVOKEVIRTUAL);
	}

	protected static Vector<Integer> onlyVirtualOpcode = new Vector<Integer>();
	static {
		onlyVirtualOpcode.add(Opcodes.INVOKEVIRTUAL);
	}

	protected static Vector<Integer> onlyStaticOpcode = new Vector<Integer>();
	static {
		onlyStaticOpcode.add(Opcodes.INVOKESTATIC);
	}

	protected static Replacement[] systemReplacements = new Replacement[] {

		new Replacement(allOpcodes, Constants.THREAD_TYPE, new Method("interrupt","()V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				InterruptInfo x = MetaDataInfoMaps.makeInterrupt(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(x.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("interrupt",Type.VOID_TYPE, new Type[] {  Constants.THREAD_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.OBJECT_TYPE, new Method("wait","()V")) { 
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				WaitInfo wait = MetaDataInfoMaps.makeWait(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(wait.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("wait",Type.VOID_TYPE, new Type[] {  Constants.OBJECT_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.OBJECT_TYPE, new Method("wait","(J)V")) { 
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				WaitInfo wait = MetaDataInfoMaps.makeWait(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(wait.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("wait",Type.VOID_TYPE, new Type[] {  Constants.OBJECT_TYPE, Type.LONG_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.OBJECT_TYPE, new Method("wait","(JI)V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				WaitInfo wait = MetaDataInfoMaps.makeWait(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(wait.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("wait",Type.VOID_TYPE, new Type[] {  Constants.OBJECT_TYPE, Type.LONG_TYPE, Type.INT_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.OBJECT_TYPE, new Method("notify","()V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("notify",Type.VOID_TYPE, new Type[] { Constants.OBJECT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.OBJECT_TYPE, new Method("notifyAll","()V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("notifyAll",Type.VOID_TYPE, new Type[] { Constants.OBJECT_TYPE }));
			}
		},

		new Replacement(onlyVirtualOpcode, Constants.THREAD_TYPE, new Method("start","()V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) {
				StartInfo start = MetaDataInfoMaps.makeStart(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(start.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("start",Type.VOID_TYPE, new Type[] {  Constants.THREAD_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(onlyStaticOpcode, Constants.SYSTEM_TYPE, new Method("exit","(I)V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				// exit goes to RRMain, so that we can run the usual shutdown steps.
				gen.invokeStatic(Constants.RR_MAIN_TYPE,new Method("exit",Type.VOID_TYPE, new Type[] {  Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.THREAD_TYPE, new Method("join","()V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				JoinInfo join = MetaDataInfoMaps.makeJoin(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(join.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("join",Type.VOID_TYPE, new Type[] {  Constants.THREAD_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.THREAD_TYPE, new Method("join","(J)V")) { 
			@Override
			public void replace(int opcode, RRMethodAdapter gen) {
				JoinInfo join = MetaDataInfoMaps.makeJoin(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(join.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("join",Type.VOID_TYPE, new Type[] {  Constants.THREAD_TYPE, Type.LONG_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.THREAD_TYPE, new Method("join","(JI)V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				JoinInfo join = MetaDataInfoMaps.makeJoin(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(join.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("join",Type.VOID_TYPE, new Type[] {  Constants.THREAD_TYPE, Type.LONG_TYPE, Type.INT_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(onlyStaticOpcode, Constants.THREAD_TYPE, new Method("sleep","(J)V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) {
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("sleep",Type.VOID_TYPE, new Type[] { Type.LONG_TYPE }));
			}
		},

		new Replacement(onlyStaticOpcode, Constants.THREAD_TYPE, new Method("sleep","(JI)V")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) {
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("sleep",Type.VOID_TYPE, new Type[] { Type.LONG_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(allOpcodes, Constants.THREAD_TYPE, new Method("isAlive","()Z")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				JoinInfo join = MetaDataInfoMaps.makeJoin(new SourceLocation(gen.getFileName(), gen.getFileLine(), gen.getByteCodeIndex()), gen.getMethod());
				gen.push(join.getId());
				gen.invokeStatic(Constants.MANAGER_TYPE,new Method("isAlive",Type.BOOLEAN_TYPE, new Type[] {  Constants.THREAD_TYPE, Type.INT_TYPE }));
			}
		},

		new Replacement(onlyStaticOpcode, Constants.SYSTEM_TYPE, Method.getMethod("void arraycopy(java.lang.Object, int, java.lang.Object, int, int)")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				gen.invokeStatic(Constants.RR_SYSTEM_TYPE, Method.getMethod("void arraycopy(java.lang.Object, int, java.lang.Object, int, int)"));
			}
		},

		new Replacement(onlyStaticOpcode, Constants.SYSTEM_TYPE, Method.getMethod("void gc()")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
			}
		},

		new Replacement(onlyVirtualOpcode, Constants.RUNTIME_TYPE, new Method("availableProcessors","()I")) {
			@Override
			public void replace(int opcode, RRMethodAdapter gen) { 
				gen.invokeStatic(Constants.RR_MAIN_TYPE,new Method("availableProccesors",Type.INT_TYPE, new Type[] {  Constants.OBJECT_TYPE }));
			}
		},

	};
}
