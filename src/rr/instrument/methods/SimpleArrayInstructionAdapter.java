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


import rr.RRMain;
import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.meta.ArrayAccessInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.org.objectweb.asm.Label;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.Method;
import rr.state.AbstractArrayState;
import rr.tool.RR;
import acme.util.Assert;
import acme.util.Util;

public class SimpleArrayInstructionAdapter extends GuardStateInstructionAdapter implements Opcodes {

	protected int guardStateLoc;
	protected int arrayShadowLoc;
	protected int tmpLoc;
	protected int indexLoc;
	protected int arrayLoc;

	private static final Type arrayShadowType = Type.getType(AbstractArrayState.class);
	private static final Method getStateMethod = Method.getMethod("rr.state.ShadowVar getState(int)");
	private static final Method nextDimMethod = Method.getMethod("rr.state.AbstractArrayState getShadowForNextDim(rr.state.ShadowThread, Object, int)");

	public SimpleArrayInstructionAdapter(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
		guardStateLoc = context.getNextFreeVar(1);
		arrayShadowLoc = context.getNextFreeVar(1);
		tmpLoc = context.getNextFreeVar(1);
		indexLoc = context.getNextFreeVar(1);
		arrayLoc = context.getNextFreeVar(1);
	}

	protected void insertFastPathCode(final Label success, boolean isWrite) {
		if (!RR.nofastPathOption.get()) {
			super.visitVarInsn(ALOAD, arrayShadowLoc);
			// target-shadow
			super.visitVarInsn(ILOAD, indexLoc);
			// index target-shadow 
			this.invokeVirtual(arrayShadowType, getStateMethod);
			// ShadowVar 
			super.visitVarInsn(ASTORE, guardStateLoc);	
		
			ASMUtil.insertArrayFastPathCode(this, isWrite, arrayShadowLoc, guardStateLoc, threadDataLoc, success, indexLoc);
		} 
	}


	@Override
	protected void visitArrayInsn(int opcode) {
		switch(opcode) {
		case AALOAD: 
		case BALOAD: 
		case CALOAD: 
		case FALOAD: 
		case IALOAD: 
		case SALOAD: 
		case DALOAD: 
		case LALOAD:	{					

			ArrayAccessInfo access = MetaDataInfoMaps.makeArrayAccess(this.getLocation(), this.getMethod(), false);
			if (!InstrumentationFilter.shouldInstrument(access)) {
				if (RRMain.slowMode()) Util.log("Skipping: " + access);
				super.visitArrayInsn(opcode);
				return;
			} 


			if (!RR.nofastPathOption.get()) {
				final Label success = new Label();
				this.visitInsn(DUP2);
				// index target index target
				this.push(access.getId());
				// arrayAccessDataid index target index target
				this.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread arrayAccessDataid index target index target  
				this.invokeStatic(Constants.MANAGER_TYPE, Method.getMethod("rr.state.AbstractArrayState arrayShadow(Object, int, int, rr.state.ShadowThread)"));
				// target-shadow index target  
				this.visitVarInsn(ASTORE, arrayShadowLoc);	
				// index target
				this.dup();
				// index index target
				this.visitVarInsn(ISTORE, indexLoc);	
				// index target
				insertFastPathCode(success, false);

				// index target
				this.visitInsn(DUP2);
				// index target index target
				this.push(access.getId());
				// arrayAccessDataid index target index target
				this.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread arrayAccessDataid index target index target  
				this.visitVarInsn(ALOAD, arrayShadowLoc);				
				// ShadowVar ShadowThread arrayAccessDataid index target index target  
				this.invokeStatic(Constants.MANAGER_TYPE, Constants.READ_ARRAY_WITH_UPDATER_METHOD);
				// index target

				this.visitLabel(success);
			} else {
				// index target
				this.visitInsn(DUP2);
				// index target index target
				this.push(access.getId());
				// arrayAccessDataid index target index target
				this.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread arrayAccessDataid index target index target  
				this.invokeStatic(Constants.MANAGER_TYPE, Constants.READ_ARRAY_METHOD);
				// index target
			} 
			super.visitArrayInsn(opcode);
			break;
		}

		case DASTORE: 
		case LASTORE: 
		case AASTORE: 
		case BASTORE: 
		case CASTORE: 
		case FASTORE: 
		case IASTORE: 
		case SASTORE: {

			ArrayAccessInfo access = MetaDataInfoMaps.makeArrayAccess(this.getLocation(), this.getMethod(), true);
			if (!InstrumentationFilter.shouldInstrument(access)) {
				if (RRMain.slowMode()) Util.log("Skipping: " + access);
				super.visitArrayInsn(opcode);
				return;
			} 

			boolean doubleSize = (opcode == DASTORE || opcode == LASTORE); 
			if (doubleSize) {
				// value index target 
				this.visitInsn(DUP2_X2);
				// value index target value
				this.visitInsn(POP2);
				// index target value
			} else {
				// value index target 
				this.visitInsn(DUP_X2);
				// value index target value
				this.visitInsn(POP);
				// index target value 
			}

			if (!RR.nofastPathOption.get()) {
				final Label success = new Label();
				this.visitInsn(DUP2);
				// index target index target value
				this.push(access.getId());
				// arrayAccessDataid index target index target value
				this.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread arrayAccessDataid index target index target value  
				this.invokeStatic(Constants.MANAGER_TYPE, Method.getMethod("rr.state.AbstractArrayState arrayShadow(Object, int, int, rr.state.ShadowThread)"));
				// target-shadow index target value
				this.visitVarInsn(ASTORE, arrayShadowLoc);	
				// index target value
				this.dup();
				// index index target value
				this.visitVarInsn(ISTORE, indexLoc);
				// index target value
				insertFastPathCode(success, true);

				// index target value
				this.visitInsn(DUP2);
				// index target index target value
				this.push(access.getId());
				// arrayAccessDataid index target index target value
				this.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread arrayAccessDataid index target index target value  
				this.visitVarInsn(ALOAD, arrayShadowLoc);				
				// ShadowVar ShadowThread arrayAccessDataid index target index target value  
				this.invokeStatic(Constants.MANAGER_TYPE, Constants.WRITE_ARRAY_WITH_UPDATER_METHOD);
				// index target value
				this.visitLabel(success);
			} else {

				this.visitInsn(DUP2);
				// index target index target value
				push(access.getId());
				// arrayAccessDataid index target index target value
				this.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread arrayAccessDataid index target index target value
				this.invokeStatic(Constants.MANAGER_TYPE, Constants.WRITE_ARRAY_METHOD);
				// index target value
			}

			if (doubleSize) {
				// index target value value
				this.visitInsn(DUP2_X2);
				// index target value value index target
				this.visitInsn(POP2);
				// value value index target
			} else {
				// index target value 
				this.visitInsn(DUP2_X1);
				// index target value index target
				this.visitInsn(POP2);
				// value index target
			}
			super.visitArrayInsn(opcode);
			break;
		}
		default:
			Assert.panic("Not an target opcode!");
		}	
	}
}