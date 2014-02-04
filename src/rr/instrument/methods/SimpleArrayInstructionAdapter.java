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

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.ArrayAccessInfo;
import rr.meta.MethodInfo;
import rr.state.AbstractArrayState;
import rr.tool.RR;
import acme.util.Assert;
import acme.util.Util;

public class SimpleArrayInstructionAdapter extends GuardStateInstructionAdapter implements Opcodes {
	
	public SimpleArrayInstructionAdapter(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
	}

	private void putAccessed(int opcode){
		if(opcode == AALOAD || opcode == AASTORE){
			//push reference at index of target (if reference type) and null otherwise
			//called on mv (not this) so this.visitArrayInsn is not called as a consequence
			mv.visitInsn(AALOAD);
			
			//accessedReference index target
			this.visitVarInsn(ASTORE, threadDataLoc + 1);
			//index target
			this.visitInsn(DUP2);
			//index target index target
			this.visitVarInsn(ALOAD, threadDataLoc + 1);
			//accessedReference index target index target
		}
		else{
			this.visitInsn(ACONST_NULL);
			//null index target index target
		}
		
		return;
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
					Util.log("Skipping: " + access);
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
					this.dup2();
					// target-shadow index target-shadow index target
					this.visitVarInsn(ASTORE, threadDataLoc + 5);	
					// index target-shadow index target
					this.invokeVirtual(Type.getType(AbstractArrayState.class), Method.getMethod("rr.state.ShadowVar getState(int)"));
					// ShadowVar index target
					this.visitVarInsn(ASTORE, threadDataLoc + 3);	

					ASMUtil.insertFastPathCode(this, false, threadDataLoc + 3, threadDataLoc, success);

					// index target
					this.visitInsn(DUP2);
					
					// index target index target
					putAccessed(opcode);
					
					//accessedReference index target index target
					this.push(access.getId());
					// arrayAccessDataid accessedReference index target index target
					this.visitVarInsn(ALOAD, threadDataLoc);				
					// ShadowThread arrayAccessDataid index accessedReference target index target  
					this.visitVarInsn(ALOAD, threadDataLoc+5);				
					// ShadowVar ShadowThread arrayAccessDataid accessedReference index target index target  
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.READ_ARRAY_WITH_UPDATER_METHOD);
					// index target

					this.visitLabel(success);
				} else {
					// index target
					this.visitInsn(DUP2);
					
					// index target index target
					putAccessed(opcode);
					//accessedReference index target index target
					this.push(access.getId());
					// arrayAccessDataid accessedReference index target index target
					this.visitVarInsn(ALOAD, threadDataLoc);				
					// ShadowThread arrayAccessDataid accessedReference index target index target  
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
					Util.log("Skipping: " + access);
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
					// target-shadow index target   value
					this.dup2();
					// target-shadow index target-shadow index target value
					this.visitVarInsn(ASTORE, threadDataLoc + 5);	
					// index target-shadow index target value
					this.invokeVirtual(Type.getType(rr.state.AbstractArrayState.class), Method.getMethod("rr.state.ShadowVar getState(int)"));
					// ShadowVar index target value
					this.visitVarInsn(ASTORE, threadDataLoc + 3);	
					// index target value
					ASMUtil.insertFastPathCode(this, true, threadDataLoc + 3, threadDataLoc, success);

					// index target value
					this.visitInsn(DUP2);
					
					
					
					// index target index target value
					putAccessed(opcode);
					//accessedReference index target index target value
					this.push(access.getId());
					// arrayAccessDataid accessedReference index target index target value
					this.visitVarInsn(ALOAD, threadDataLoc);				
					// ShadowThread arrayAccessDataid accessedReference index target index target value  
					this.visitVarInsn(ALOAD, threadDataLoc+5);				
					// ShadowVar ShadowThread arrayAccessDataid accessedReference index target index target value  
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.WRITE_ARRAY_WITH_UPDATER_METHOD);
					// index target value
					this.visitLabel(success);
				} else {
					this.visitInsn(DUP2);
					
					
					// index target index target value
					putAccessed(opcode);
					
					
					//accessedReference index target index target value
					push(access.getId());
					// arrayAccessDataid accessedReference index target index target value
					this.visitVarInsn(ALOAD, threadDataLoc);				
					// ShadowThread arrayAccessDataid accessedReference index target index target value
					this.invokeStatic(Constants.MANAGER_TYPE, Constants.WRITE_ARRAY_METHOD);
					// index target value
				}
				if (doubleSize) {
					// index target value value
					this.visitInsn(DUP2_X2);
					// index target value value index target
					this.visitInsn(POP2);
				} else {
					// index target value 
					this.visitInsn(DUP2_X1);
					// index target value index target
					this.visitInsn(POP2);
				}
				super.visitArrayInsn(opcode);
				break;
			}
			default:
				Assert.panic("Not an target opcode!");
		}
	}
}