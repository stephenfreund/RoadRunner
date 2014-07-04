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
import rr.org.objectweb.asm.commons.AnalyzerAdapter;
import rr.org.objectweb.asm.commons.Method;

import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.ArrayAccessInfo;
import rr.meta.MethodInfo;
import acme.util.Assert;
import acme.util.Util;

public class SimpleArrayWithValuesInstructionAdapter extends GuardStateInstructionAdapter implements Opcodes {

	protected AnalyzerAdapter types;

	public SimpleArrayWithValuesInstructionAdapter(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
	}
	
	public void setTypeAnalyzer(AnalyzerAdapter types) {
		this.types = types;
	}

	protected Type typeForOpcode(int opcode) {
		switch(opcode) {
		case AASTORE: 
		case AALOAD: return Constants.OBJECT_TYPE;
		case BASTORE: 
		case BALOAD: return Type.BYTE_TYPE;
		case CASTORE: 
		case CALOAD: return Type.CHAR_TYPE;
		case FASTORE: 
		case FALOAD: return Type.FLOAT_TYPE;
		case IASTORE: 
		case IALOAD: return Type.INT_TYPE;
		case SASTORE: 
		case SALOAD: return Type.SHORT_TYPE;
		case DASTORE: 
		case DALOAD: return Type.DOUBLE_TYPE;
		case LASTORE: 
		case LALOAD: return Type.LONG_TYPE;						
		}
		Assert.panic("Fall Through " + opcode);
		return null;
	}

	@Override
	protected void visitArrayInsn(int opcode) {
		Type typeForOpcode = this.typeForOpcode(opcode);
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
				this.arrayLoad(typeForOpcode);
				return;
			} 
			
			String desc = types.stack.get(types.stack.size() - 2).toString();
//			Util.log("STACK " + types.stack + "--- LOCALS " + types.locals);
//			Util.log("DESC=" + desc);
			Type arrayType = Type.getType(desc);	 
//			Util.log(typeForOpcode + " " + arrayType);


			// index target
			this.visitInsn(DUP2);
			// index target index target
			this.arrayLoad(typeForOpcode);
			// value index target
			this.visitVarInsn(ASMUtil.storeInstr(typeForOpcode), threadDataLoc + 7);
			//  index target
			this.push(access.getId());
			// arrayAccessDataid index target 
			this.visitVarInsn(ALOAD, threadDataLoc);				
			// ShadowThread arrayAccessDataid index target 
			this.visitVarInsn(ASMUtil.loadInstr(typeForOpcode), threadDataLoc + 7);
			// value ShadowThread arrayAccessDataid index target 
			final Method m = Constants.getReadArrayAccessWithValueMethod(typeForOpcode);
			this.invokeStatic(Constants.MANAGER_VALUE_TYPE, m);
			// value
			Type elementType = Type.getType(arrayType.getDescriptor().substring(1));
//			Util.log(typeForOpcode + " " + elementType);

			if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
				this.checkCast(elementType);
			}

			break;
		}

		
		case AASTORE: 
		case DASTORE: 
		case LASTORE: 
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

//			Util.log(types.stack);
			String desc = types.stack.get(types.stack.size() - 2 - typeForOpcode.getSize()).toString();
			Type arrayType = Type.getType(desc);	

//			Util.log(typeForOpcode + " " + arrayType);

			// value index target 
			this.visitVarInsn(ASMUtil.storeInstr(typeForOpcode), threadDataLoc + 7);
			// index target 
			this.dup2();
			// index target index target 
			this.arrayLoad(typeForOpcode);
			// oldvalue index target
			this.visitVarInsn(ASMUtil.storeInstr(typeForOpcode), threadDataLoc + 9);
			// index target 
			this.dup2();
			// index target index target 
			push(access.getId());
			// arrayAccessDataid index target index target 
			this.visitVarInsn(ALOAD, threadDataLoc);				
			// ShadowThread arrayAccessDataid index target index target
			this.visitVarInsn(ASMUtil.loadInstr(typeForOpcode), threadDataLoc + 9);
			// oldvalue ShadowThread arrayAccessDataid index target index target
			this.visitVarInsn(ASMUtil.loadInstr(typeForOpcode), threadDataLoc + 7);
			// newvalue oldvalue ShadowThread arrayAccessDataid index target index target
			final Method m = Constants.getWriteArrayAccessWithValueMethod(typeForOpcode);
			this.invokeStatic(Constants.MANAGER_VALUE_TYPE, m);
			
			Type elementType = Type.getType(arrayType.getDescriptor().substring(1));
//			Util.log(typeForOpcode + " " + elementType);

			if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
				this.checkCast(elementType);
			}
			// value index target 
			this.arrayStore(typeForOpcode);
			break;
		}
		default:
			Assert.panic("Not an target opcode! "  + opcode);
		}	
	}
}
