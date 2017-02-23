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

import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.instrument.Instrumentor;
import rr.loader.MethodResolutionException;
import rr.loader.RRTypeInfo;
import rr.meta.AcquireInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.InvokeInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.meta.ReleaseInfo;
import rr.org.objectweb.asm.Label;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public class ThreadDataInstructionAdapter extends RRMethodAdapter implements Opcodes {


	public static final CommandLineOption<Boolean> callSitesOption = 
		CommandLine.makeBoolean("callSites", false, CommandLineOption.Kind.EXPERIMENTAL, "Track Call Site Info.");


	protected int threadDataLoc;
	protected int threadDataParamLoc;  // either the param for passed in thread state, or -1

	public ThreadDataInstructionAdapter(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
		threadDataParamLoc = ASMUtil.locOfThreadData(m.getDescriptor(), m.isStatic());
		threadDataLoc = context.getThreadDataVar();
	}


	@Override
	public void visitVarInsn(int opcode, int var) {
		var = adjustVar(var);
		super.visitVarInsn(opcode, var);
	}

	@Override
	public void visitIincInsn(int var, int increment) {
		var = adjustVar(var);
		super.visitIincInsn(var, increment);
	}


	private int adjustVar(int var) {
		if (threadDataParamLoc != -1 && var >= threadDataParamLoc && var < context.getThreadDataVar()) {
			var++;
		}
		return var;
	}

	public boolean is_getCurrentThread(int opcode, String owner, String name, String desc) {
		return opcode == Opcodes.INVOKESTATIC &&
		Type.getObjectType(owner).equals(Constants.THREAD_STATE_TYPE) &&
		name.equals(Constants.CURRENT_THREAD_METHOD.getName()) &&
		desc.equals(Constants.CURRENT_THREAD_METHOD.getDescriptor());
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) {

		if (callSitesOption.get()) {
			try {
				MethodInfo m = RRTypeInfo.resolveMethodDescriptor(owner, name, desc);
				if (!m.isSynthetic() && !m.getOwner().getName().startsWith("rr/")) {
					InvokeInfo i = MetaDataInfoMaps.makeInvoke(this.getLocation(), m, context.getMethod());
					this.visitLdcInsn(i.getId());
					this.visitVarInsn(ALOAD, threadDataLoc);
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.INVOKE_METHOD);
				}
			} catch (MethodResolutionException e) {
				Yikes.yikes("Can't find method in Thread Data Instruction Adapter: " + e);
			}
		}

		if (opcode != Opcodes.INVOKEINTERFACE && !owner.startsWith("[")) {
			MethodInfo m;
			try {
				m = RRTypeInfo.resolveMethodDescriptor(owner, name, desc);
				if (is_getCurrentThread(opcode, owner, name, desc)) {
					this.visitVarInsn(ALOAD, threadDataLoc);
				} else if (InstrumentationFilter.supportsThreadStateParam(m)) {
					String newDesc = ASMUtil.addThreadDataToDescriptor(desc);
					this.visitVarInsn(ALOAD, threadDataLoc);
					super.visitMethodInsn(opcode, owner, Constants.getThreadLocalName(name), newDesc, isInterface);
				} else {
					super.visitMethodInsn(opcode, owner, name, desc, isInterface);
				}
			} catch (MethodResolutionException e) {
				Yikes.yikes("Can't find method in Thread Data Instruction Adapter: " + e);
				super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			}
		} else {
			super.visitMethodInsn(opcode, owner, name, desc, isInterface);
		}
	}

	@Override
	public void visitCode() {
		mv.visitCode();
		if (threadDataParamLoc == -1) {
			this.invokeStatic(Constants.THREAD_STATE_TYPE, Constants.CURRENT_THREAD_METHOD);
		} else {
			super.visitVarInsn(ALOAD, threadDataParamLoc);
		}
		super.visitVarInsn(ASTORE, threadDataLoc);
	}	


	protected void visitArrayInsn(int opcode) {
		super.visitInsn(opcode);
	}

	@Override
	public void visitInsn(int opcode) {
		final MethodInfo method = context.getMethod();
		if (InstrumentationFilter.shouldInstrument(method)) {
			switch(opcode) {
			case MONITORENTER: {
				AcquireInfo acquire = MetaDataInfoMaps.makeAcquire(this.getLocation(), method);
				if (!Instrumentor.useTestAcquireOption.get()) {
					/* Simple Version: */
					// traget
					visitInsn(DUP);
					// target target
					super.visitInsn(MONITORENTER);
					// target
					push(acquire.getId());
					// syncNum targe
					this.visitVarInsn(ALOAD, threadDataLoc);
					// ShadowThread syncNum targe 
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.ACQUIRE_METHOD);
				} else {
					/* with testAcquire... */
					visitInsn(DUP);
					// targe targe
					push(acquire.getId());
					// syncNum targe targe
					this.visitVarInsn(ALOAD, threadDataLoc);
					// ShadowThread syncNum targe targe
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.TEST_ACQUIRE_METHOD);
					// boolean targe
					Label lSkip = new Label();
					Label lEnd = new Label();
					this.ifZCmp(IFEQ, lSkip);
					// targe
					this.visitInsn(DUP);
					// targe targe
					super.visitInsn(MONITORENTER);
					// targe
					push(acquire.getId());
					// syncNum targe
					this.visitVarInsn(ALOAD, threadDataLoc);
					// threadData syncNum targe
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.ACQUIRE_METHOD);
					super.goTo(lEnd);
					this.visitLabel(lSkip);
					visitInsn(POP);
					visitLabel(lEnd);
				}
				break;
			}
			case MONITOREXIT: {
				ReleaseInfo release = MetaDataInfoMaps.makeRelease(this.getLocation(), method);

				if (!Instrumentor.useTestAcquireOption.get()) {
					/* Simple Version: */
					visitInsn(DUP);
					// targe targe
					push(release.getId());
					// syncNum targe targe
					this.visitVarInsn(ALOAD, threadDataLoc);
					// ShadowThread syncNum targe targe
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.RELEASE_METHOD);
					// targe
					super.visitInsn(MONITOREXIT);
				} else {
					visitInsn(DUP);
					// targe targe
					push(release.getId());
					// syncNum targe targe
					this.visitVarInsn(ALOAD, threadDataLoc);
					// ShadowThread syncNum targe targe
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.TEST_RELEASE_METHOD);
					// boolean target
					Label lSkip = new Label();
					Label lEnd = new Label();
					this.ifZCmp(IFEQ, lSkip);
					// targe
					this.visitInsn(DUP);
					// targe targe
					push(release.getId());
					// syncNum targe targe
					this.visitVarInsn(ALOAD, threadDataLoc);				
					// threadData syncNum targe targe
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.RELEASE_METHOD);
					// targe
					super.visitInsn(MONITOREXIT);
					super.goTo(lEnd);
					this.visitLabel(lSkip);
					visitInsn(POP);
					visitLabel(lEnd);
				}
				break;
			} 
			case AALOAD: 
			case BALOAD: 
			case CALOAD: 
			case FALOAD: 
			case IALOAD: 
			case SALOAD: 
			case DALOAD: 
			case LALOAD:	
			case DASTORE: 
			case LASTORE: 
			case AASTORE: 
			case BASTORE: 
			case CASTORE: 
			case FASTORE: 
			case IASTORE: 
			case SASTORE: 
				this.visitArrayInsn(opcode);
				break;
			default:
				super.visitInsn(opcode);
			}
		} else {	
			super.visitInsn(opcode);
		}
	}
}
