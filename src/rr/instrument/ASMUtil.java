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

package rr.instrument;

import rr.meta.FieldInfo;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.instrument.methods.RRMethodAdapter;
import rr.org.objectweb.asm.Label;
import rr.tool.Tool;
import rr.tool.RR;
import rr.tool.ToolVisitor;
import acme.util.Assert;


public class ASMUtil implements Opcodes {

	public static int size(String desc) {
		Type t = Type.getType(desc);
		return t.getSize();
	}

	public static int storeInstr(Type t) {
		switch (t.getSort()) {
		case Type.VOID: Assert.fail("Bad Sort");
		case Type.BOOLEAN:
		case Type.CHAR:
		case Type.BYTE:
		case Type.SHORT:
		case Type.INT: return ISTORE;
		case Type.FLOAT: return FSTORE;
		case Type.LONG: return LSTORE;
		case Type.DOUBLE: return DSTORE;
		case Type.ARRAY:
		case Type.OBJECT: return ASTORE;
		}
		return -1;
	}

	public static int storeInstr(String desc) {
		Type t = Type.getType(desc);
		return storeInstr(t);
	}

	public static int loadInstr(Type t) {
		switch (t.getSort()) {
		case Type.VOID: Assert.fail("Bad Sort");
		case Type.BOOLEAN:
		case Type.CHAR:
		case Type.BYTE:
		case Type.SHORT:
		case Type.INT: return ILOAD;
		case Type.FLOAT: return FLOAD;
		case Type.LONG: return LLOAD;
		case Type.DOUBLE: return DLOAD;
		case Type.ARRAY:
		case Type.OBJECT: return ALOAD;
		}
		Assert.fail("Bad Sort");
		return -1;
	}

	public static int loadInstr(String desc) {
		Type t = Type.getType(desc);
		return loadInstr(t);
	}

	public static int returnInstr(Type t) {
		switch (t.getSort()) {
		case Type.VOID: return RETURN;
		case Type.BOOLEAN:
		case Type.CHAR:
		case Type.BYTE:
		case Type.SHORT:
		case Type.INT: return IRETURN;
		case Type.FLOAT: return FRETURN;
		case Type.LONG: return LRETURN;
		case Type.DOUBLE: return DRETURN;
		case Type.ARRAY:
		case Type.OBJECT: return ARETURN;
		}
		Assert.fail("Bad Sort");
		return -1;
	}

	public static int returnInstr(String desc) {
		Type t = Type.getType(desc);
		return returnInstr(t);
	}

	public static int argLength(String desc) {
		Type args[] = Type.getArgumentTypes(desc);
		int paramLength = 0;
		for (Type t : args) {
			paramLength += t.getSize();
		}
		return paramLength;
	}

	public static int locOfThreadData(String desc, boolean isStatic) {
		Type args[] = Type.getArgumentTypes(desc);
		if (args.length == 0 || !args[args.length - 1].equals(Constants.THREAD_STATE_TYPE)) {
			return -1;
		}
		int argLoc = 0;
		if (!isStatic) argLoc++;
		for (int i = 0; i < args.length - 1; i++) {
			argLoc += args[i].getSize();
		}
		return argLoc;
	}

	public static void callMethodInSameClass(String owner, String name, String desc, MethodVisitor mv, int access) {
		if ((access & ACC_STATIC) == 0) {
			mv.visitVarInsn(ALOAD, 0);
			Type args[] = Type.getArgumentTypes(desc);
			int localVarIndex = 1;
			for (int i = 0; i < args.length; i++) {
				mv.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
				localVarIndex += args[i].getSize();

			} 
			mv.visitMethodInsn(INVOKESPECIAL, owner, name, desc, false);
		} else {
			Type args[] = Type.getArgumentTypes(desc);
			int localVarIndex = 0;
			for (int i = 0; i < args.length; i++) {
				mv.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
				localVarIndex += args[i].getSize();

			}
			mv.visitMethodInsn(INVOKESTATIC, owner, name, desc, false);
		}
	}

	// assumes this already on stack, first arg is at indexOfFistArg. 
	public static void callMethodInOtherClass(int opcode, String owner, String name, String desc, MethodVisitor mv, int indexOfFistArg) {
		Type args[] = Type.getArgumentTypes(desc);
		int localVarIndex = indexOfFistArg;
		for (int i = 0; i < args.length; i++) {
			mv.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
			localVarIndex += args[i].getSize();

		} 
		mv.visitMethodInsn(opcode, owner, name, desc, false);
	}


	public static String addThreadDataToDescriptor(String desc) {
		return addTypeToDescriptor(desc, Constants.THREAD_STATE_TYPE, Type.getArgumentTypes(desc).length);
	}

	public static String addTypeToDescriptor(String desc, Type type, int loc) {
		Type args[] = Type.getArgumentTypes(desc);
		Type ret = Type.getReturnType(desc);
		Type newArgs[] = new Type[args.length + 1];
		for (int i = 0; i < loc; i++) {
			newArgs[i] = args[i];
		}
		newArgs[loc] = type;
		for (int i = loc + 1; i < args.length + 1; i++) {
			newArgs[i] = args[i-1];
		}
		String newDesc = Type.getMethodDescriptor(ret, newArgs);
		return newDesc;
	}

	// Register 0 must have reciever if using OffsetFPMethod.
	public static void insertFastPathCode(final RRMethodAdapter mv, final boolean isWrite, final int gsVar, final int tdVar, final Label success, final FieldInfo field) {
		if (!RR.nofastPathOption.get()) {
			Label nullVarState = new Label();
			mv.visitVarInsn(ALOAD, gsVar);
			mv.visitJumpInsn(IFNULL, nullVarState);
			RR.applyToTools(new ToolVisitor() {
				public void apply(Tool t) {
					if (t.hasFieldFPMethod(isWrite)) {
						mv.push(field.getFieldOffset());
						mv.visitVarInsn(ALOAD, gsVar);
						mv.visitVarInsn(ALOAD, tdVar);
						mv.invokeStatic(Type.getType(t.getClass()), 
								isWrite ? Constants.FIELD_WRITE_FP_METHOD : Constants.FIELD_READ_FP_METHOD);
						mv.visitJumpInsn(IFNE, success);
					} else if (t.hasFPMethod(isWrite)) {
						mv.visitVarInsn(ALOAD, gsVar);
						mv.visitVarInsn(ALOAD, tdVar);
						mv.invokeStatic(Type.getType(t.getClass()), 
								isWrite ? Constants.WRITE_FP_METHOD : Constants.READ_FP_METHOD);
						mv.visitJumpInsn(IFNE, success);
					}
				}
			});
			mv.visitLabel(nullVarState);
		}
	}


	// indexVar == -1 -> on stack
	public static void insertArrayFastPathCode(final RRMethodAdapter mv, final boolean isWrite, final int shadowStateVar, final int guardStateLoc, final int tdVar, final Label success, final int indexVar) {
		if (!RR.nofastPathOption.get()) {
			
			Label nullVarState = new Label();
			mv.visitVarInsn(ALOAD, guardStateLoc);
			mv.visitJumpInsn(IFNULL, nullVarState);

			RR.applyToTools(new ToolVisitor() {
				public void apply(Tool t) {
					if (t.hasArrayFPMethod(isWrite)) {
						if (indexVar == -1) {
							mv.dup();
						} else {
							mv.visitVarInsn(ILOAD, indexVar);
						}
						mv.visitVarInsn(ALOAD, guardStateLoc);
						mv.visitVarInsn(ALOAD, tdVar);
						mv.invokeStatic(Type.getType(t.getClass()), 
								isWrite ? Constants.ARRAY_WRITE_FP_METHOD : Constants.ARRAY_READ_FP_METHOD);
						mv.visitJumpInsn(IFNE, success);
					} else if (t.hasFPMethod(isWrite)) {
						mv.visitVarInsn(ALOAD, guardStateLoc);
						mv.visitVarInsn(ALOAD, tdVar);
						mv.invokeStatic(Type.getType(t.getClass()), 
								isWrite ? Constants.WRITE_FP_METHOD : Constants.READ_FP_METHOD);
						mv.visitJumpInsn(IFNE, success);
					} 
				}
			});

			mv.visitLabel(nullVarState);

		}
	}


	public static int makePublic(int access) {
		return (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
	}
}
