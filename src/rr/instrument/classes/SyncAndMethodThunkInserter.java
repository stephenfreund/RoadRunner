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

import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.instrument.Instrumentor;
import rr.instrument.methods.RRMethodAdapter;
import rr.meta.ClassInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.org.objectweb.asm.Label;
import rr.tool.RR;

public class SyncAndMethodThunkInserter extends RRClassAdapter implements Opcodes {

	protected final ClassVisitor cvForThunks;

	public SyncAndMethodThunkInserter(final ClassVisitor cv, final ClassVisitor cvForThunks) {
		super(cv);
		this.cvForThunks = cvForThunks;
	}

	@Override
	public void visit(
			final int version,
			final int access,
			final String name,
			final String signature,
			final String superName,
			final String[] interfaces)
	{
		super.visit(version, access, name, signature, superName, interfaces);
		cvForThunks.visit(version, access, name, signature, superName, interfaces);
	}


	@Override
	public void visitSource(String source,
			String debug) {
		super.visitSource(source, debug);
		cvForThunks.visitSource(source, debug);
	}


	@Override
	public MethodVisitor visitMethod(
			final int access,
			final String name,
			final String desc,
			final String signature,
			final String[] exceptions)
	{
		final ClassInfo owner = this.getCurrentClass();
		MethodInfo method = MetaDataInfoMaps.getMethod(owner, name, desc);
		boolean instrumentMethod = 	InstrumentationFilter.shouldInstrument(method);

		if (!instrumentMethod) {
			return super.visitMethod(access, name, desc, signature, exceptions);
		}

		if (name.startsWith("<")) {
			return super.visitMethod(access,
					name,
					desc,
					signature,
					exceptions);
		} else {
			String newName = Constants.getOrigName(name);

			final String desc2 = desc; // ASMUtil.addThreadDataToDescriptor(desc);
			final MethodInfo newMethod = MetaDataInfoMaps.getMethod(owner, newName, desc2);
			newMethod.setFlags(method.isStatic(), false, method.isSynchronized());
			final int maxVar = Instrumentor.methodContext.get(method).getMaxVar();
			Instrumentor.methodContext.get(newMethod).setFirstFreeVar(maxVar+1);
			MethodVisitor mv = cv.visitMethod(access & ~ACC_SYNCHRONIZED,
					newName,
					desc2,
					signature,
					exceptions);
			
			return new ThunkMethodVisitor(mv, owner.getName(), access, name, desc2, signature, exceptions);
		}
	}

	class ThunkMethodVisitor extends MethodVisitor {
		protected String owner;
		protected int access;
		protected String name;
		protected String desc;
		protected String signature;
		protected String[] exceptions;

		protected int startLine = -1, endLine = -1;

		public ThunkMethodVisitor(MethodVisitor mv, String owner, int access, String name, String desc, String signature, String[] exceptions) {
			super(Opcodes.ASM5, mv);
			this.owner = owner;
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.signature = signature;
			this.exceptions = exceptions;
		}

		@Override
		public void visitLineNumber(int line, Label l) {
			super.visitLineNumber(line, l);
			if (startLine == -1) startLine = line;
			endLine = line;
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			final MethodInfo method = MetaDataInfoMaps.getMethod(MetaDataInfoMaps.getClass(owner), name, desc);
			boolean shouldInstrument = InstrumentationFilter.shouldInstrument(method);
			boolean isSync   = (access & ACC_SYNCHRONIZED) != 0;
			int maxLocals = Instrumentor.methodContext.get(method).getMaxVar();

			if (isSync || shouldInstrument) {
				if (isSync && shouldInstrument) {
					createSyncThunk(access  & ~ACC_SYNCHRONIZED,
							Constants.getSyncName(name),
							desc,
							signature,
							exceptions,
							Constants.getOrigName(name), 
							maxLocals);

					createMethodThunk(access  & ~ACC_SYNCHRONIZED,
							name,
							desc,
							signature,
							exceptions,
							Constants.getSyncName(name), 
							maxLocals);
				} else if (isSync) {
					createSyncThunk(access  & ~ACC_SYNCHRONIZED,
							name,
							desc,
							signature,
							exceptions,
							Constants.getOrigName(name),
							maxLocals);
				} else {
					createMethodThunk(access,
							name,
							desc,
							signature,
							exceptions,
							Constants.getOrigName(name),
							maxLocals);
				}
			}
		}



		private void createSyncThunk(int access, String name, String desc, String signature, String[] exceptions, String wrappedMethodName, int maxLocals) {
			final MethodInfo method = MetaDataInfoMaps.getMethod(MetaDataInfoMaps.getClass(owner), name, desc);
			method.setFlags((access & ACC_STATIC) != 0, false, false);
			Instrumentor.methodContext.get(method).setFirstFreeVar(maxLocals + 1);

			MethodVisitor omv = cvForThunks.visitMethod(access, name, desc, signature, exceptions);
			RRMethodAdapter mv = new RRMethodAdapter(omv, method);

			mv.visitCode();

			Label returnStatement = new Label();
			Label acquiredLock = new Label();
			Label normalExit = new Label();
			Label doneAcquiring = new Label();
			Label handler = new Label();
			Label ldontAcquire = new Label();
			Label throwException = new Label();


			mv.visitTryCatchBlock(acquiredLock, normalExit, handler, null);

			Label start = new Label();
			mv.visitLabel(start);
			mv.visitLineNumber(startLine-1, start);

			/*
			 * Perform Acquire
			 */
			visitLockPush(access, mv);
			mv.visitInsn(MONITORENTER);
			mv.visitLabel(acquiredLock);

			/*
			 * Do Method Call
			 */
			ASMUtil.callMethodInSameClass(owner, wrappedMethodName, desc, mv, access);

			Label end = new Label();
			mv.visitLabel(end);
			mv.visitLineNumber(endLine, end);

			/*
			 * Do Release
			 */
			visitLockPush(access, mv);
			mv.visitInsn(MONITOREXIT);
			mv.visitLabel(normalExit);
			mv.visitJumpInsn(GOTO, returnStatement);

			/*
			 * Handler For Exception while lock is held
			 * Must call testRelease, release, and rethrow exception...
			 */
			mv.visitLabel(handler);

			visitLockPush(access, mv);
			mv.visitInsn(MONITOREXIT);
			mv.visitLabel(throwException);
			mv.visitInsn(ATHROW);

			mv.visitLabel(returnStatement);
			mv.visitInsn(ASMUtil.returnInstr(Type.getReturnType(desc)));
			mv.visitMaxs(20, 20);
			mv.visitEnd();
		}


		private void visitLockPush(int access, RRMethodAdapter mv) {
			if ((access & ACC_STATIC) != 0) { 
				mv.visitLdcInsn(Type.getObjectType(owner));
			} else {
				mv.visitVarInsn(ALOAD, 0);
			}
		}


		private void createMethodThunk(int access, String name, String desc, String signature, String[] exceptions, String wrappedMethodName, int maxLocals) {
			final MethodInfo method = MetaDataInfoMaps.getMethod(MetaDataInfoMaps.getClass(owner), name, desc);
			method.setFlags((access & ACC_STATIC) != 0, false, method.isSynchronized());
			Instrumentor.methodContext.get(method).setFirstFreeVar(maxLocals + 1);
			
			MethodVisitor omv = cvForThunks.visitMethod(access, name, desc, signature, exceptions);
			RRMethodAdapter mv = new RRMethodAdapter(omv, method);

			mv.visitCode();

			Label returnStatement = new Label();
			Label enteredBlock = new Label();
			Label normalExit = new Label();
			Label handler = new Label();


			Label start = new Label();
			mv.visitLabel(start);
			mv.visitLineNumber(startLine-1, start);

			/*
			 * Perform Enter
			 */
			if (!RR.noEnterOption.get()) {
				mv.visitTryCatchBlock(enteredBlock, normalExit, handler, null);

				if ((access & ACC_STATIC) != 0) { 
					mv.visitInsn(ACONST_NULL);
				} else {
					mv.visitVarInsn(ALOAD, 0);
				}
				MethodInfo m = method;
				mv.push(m.getId());
				mv.invokeStatic(Constants.THREAD_STATE_TYPE, Constants.CURRENT_THREAD_METHOD);
				mv.invokeStatic(Constants.MANAGER_TYPE, Constants.ENTER_METHOD);
				mv.visitLabel(enteredBlock);

				/*
				 * Do Method Call
				 */
				ASMUtil.callMethodInSameClass(owner, wrappedMethodName, desc, mv, access);

				/*
				 * Do Exit
				 */
				mv.invokeStatic(Constants.THREAD_STATE_TYPE, Constants.CURRENT_THREAD_METHOD);
				mv.invokeStatic(Constants.MANAGER_TYPE, Constants.EXIT_METHOD);
				mv.visitLabel(normalExit);
				mv.visitJumpInsn(GOTO, returnStatement);

				/*
				 * Handler For Exception for when inside atomic call
				 */
				mv.visitLabel(handler);
				mv.invokeStatic(Constants.THREAD_STATE_TYPE, Constants.CURRENT_THREAD_METHOD);
				mv.invokeStatic(Constants.MANAGER_TYPE, Constants.EXIT_METHOD);
				mv.visitInsn(ATHROW);
			} else {
				ASMUtil.callMethodInSameClass(owner, wrappedMethodName, desc, mv, access);	
			}
			mv.visitLabel(returnStatement);
			mv.visitInsn(ASMUtil.returnInstr(Type.getReturnType(desc)));
			mv.visitMaxs(10, mv.getContext().getMaxVar());
			mv.visitEnd();
		}
	}
}
