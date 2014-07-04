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

package rr.instrument.hooks;

import java.util.Vector;

import rr.org.objectweb.asm.ClassWriter;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.GeneratorAdapter;
import rr.org.objectweb.asm.commons.Method;

import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.instrument.methods.RRMethodAdapter;
import rr.loader.Loader;
import rr.meta.InstrumentationFilter;
import rr.meta.MethodInfo;
import rr.state.ShadowThread;
import acme.util.Util;

public class SpecialMethods implements Opcodes {

	protected static Vector<SpecialMethodCallBack> hooks = new Vector<SpecialMethodCallBack>();
	private static int thunkCount;
	
	public static SpecialMethodCallBack addHook(String classPattern, String methodString, SpecialMethodListener listener) {
		SpecialMethodCallBack hook = new SpecialMethodCallBack(classPattern, methodString);
		hook.addListener(listener);
		hooks.add(hook);
		
 		String m = hook.getKey();
 		InstrumentationFilter.methodsToWatch.get().addFirst("-" + m);

		return hook;
	}
	
	public static void invoke0(String key, boolean isPre, Object[] args, ShadowThread td) {
		for (SpecialMethodCallBack h : hooks) {
			if (h.matches(key)) {
				h.invoke(isPre, args, td);
			}
		}
	}
	
	public static void invoke(String key, boolean isPre, ShadowThread td) {
		invoke0(key, isPre, new Object[] {  }, td);
	}
	
	public static void invoke(String key, boolean isPre, Object o1, ShadowThread td) {
		invoke0(key, isPre, new Object[] { o1 }, td);
	}

	public static void invoke(String key, boolean isPre, Object o1, Object o2, ShadowThread td) {
		invoke0(key, isPre, new Object[] { o1, o2 }, td);	
	}

	public static void invoke(String key, boolean isPre, Object o1, Object o2, Object o3, ShadowThread td) {
		invoke0(key, isPre, new Object[] { o1, o2, o3 }, td);
	}

	public static void invoke(String key, boolean isPre, Object o1, Object o2, Object o3, Object o4, ShadowThread td) {
		invoke0(key, isPre, new Object[] { o1, o2, o3, o4 }, td);
	}
	
	public static void invoke(String key, boolean isPre, Object o1, Object o2, Object o3, Object o4, Object o5, ShadowThread td) {
		invoke0(key, isPre, new Object[] { o1, o2, o3, o4, o5 }, td);
	}
	
	protected static final String descriptors[] = {
		"(Ljava/lang/String;ZLrr/state/ShadowThread;)V",
		"(Ljava/lang/String;ZLjava/lang/Object;Lrr/state/ShadowThread;)V",
		"(Ljava/lang/String;ZLjava/lang/Object;Ljava/lang/Object;Lrr/state/ShadowThread;)V",
		"(Ljava/lang/String;ZLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lrr/state/ShadowThread;)V",
		"(Ljava/lang/String;ZLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lrr/state/ShadowThread;)V",
		"(Ljava/lang/String;ZLjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Lrr/state/ShadowThread;)V",
	};
	
	public static boolean anyMatches(MethodInfo m) {
		for (SpecialMethodCallBack h : hooks) {
			if (h.matches(m.getKey())) {
				return true;
			}
		}
		return false;
	}


	public static boolean tryReplace(RRMethodAdapter gen, int opcode, MethodInfo method, MethodInfo enclosing) {

		if (!anyMatches(method)) {
			return false;
		}

		Util.logf("Creating listener specific replacement for %s", method);

		final Type thunkType;
		String className = "__$rr_TSRThunk_" + enclosing.getOwner().getName().replace('/', '_') + "_" + thunkCount++;
		thunkType = Type.getObjectType(className);
		Method invokeMethod = new Method("invoke", method.getDescriptor());
		invokeMethod = new Method("invoke", ASMUtil.addTypeToDescriptor(invokeMethod.getDescriptor(), Type.getObjectType(method.getOwner().getName().replace('.','/')), 0));

		gen.invokeStatic(thunkType, invokeMethod);

		ClassWriter cw = new ClassWriter(0);
		GeneratorAdapter mv;
		cw.visit(V1_4, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		{
			mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null), ACC_PUBLIC, "<init>",  "()V");
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}

		{
			mv = new GeneratorAdapter(cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "invoke", invokeMethod.getDescriptor(), null, null), ACC_PUBLIC + ACC_STATIC, "invoke", invokeMethod.getDescriptor());
			mv.visitCode();
			String key = method.getKey();
			
			callWithPromotionToObject(mv, key, true, invokeMethod);
			
			if (opcode == INVOKEVIRTUAL) {
				mv.visitVarInsn(ALOAD, 0);
				ASMUtil.callMethodInOtherClass(INVOKEVIRTUAL, method.getOwner().getName(), method.getName(), method.getDescriptor(), mv, 1);
			} else {
				ASMUtil.callMethodInOtherClass(INVOKESTATIC, method.getOwner().getName(), method.getName(), method.getDescriptor(), mv, 0);
			}

			callWithPromotionToObject(mv, key, false, invokeMethod);

			mv.visitInsn(ASMUtil.returnInstr(invokeMethod.getReturnType())); 
			mv.visitMaxs(20, 20);
			mv.visitEnd();
		}

		cw.visitEnd();		
		byte[] byteArray = cw.toByteArray();
		Loader.writeToFileCache("tsr", className, byteArray);
		Loader.loaderForClass(gen.getMethod().getOwner().getName()).defineClass(className, byteArray);
		return true;

	}



	private static void callWithPromotionToObject(GeneratorAdapter mv, String key, boolean isPre, Method m) {
		Type args[] = m.getArgumentTypes();
		int localVarIndex = 0;
		mv.push(key);
		mv.push(isPre);
		for (int i = 0; i < args.length; i++) {
			mv.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
			mv.box(args[i]);
			localVarIndex += args[i].getSize();

		} 
		mv.visitMethodInsn(Opcodes.INVOKESTATIC,
							Constants.THREAD_STATE_TYPE.getInternalName(),
							Constants.CURRENT_THREAD_METHOD.getName(),
							Constants.CURRENT_THREAD_METHOD.getDescriptor(), false);
		mv.visitMethodInsn(INVOKESTATIC, "rr/instrument/hooks/SpecialMethods", "invoke", descriptors[args.length], false);
	}
	
}
