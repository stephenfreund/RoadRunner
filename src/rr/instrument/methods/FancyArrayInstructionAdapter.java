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

import java.util.HashMap;

import rr.RRMain;
import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.instrument.analysis.MethodVisitorWithAnalysisFrames;
import rr.instrument.array.ArrayShadowFrame;
import rr.instrument.array.ArrayShadowValue;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.ArrayAccessInfo;
import rr.meta.MethodInfo;
import rr.org.objectweb.asm.Label;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.Method;
import rr.org.objectweb.asm.tree.analysis.Frame;
import rr.state.AbstractArrayState;
import rr.state.AbstractArrayStateCache;
import rr.state.ArrayStateCache;
import rr.state.ArrayStateFactory;
import rr.tool.RR;
import rr.tool.Tool;
import rr.tool.ToolVisitor;
import acme.util.Assert;
import acme.util.Util;

public class FancyArrayInstructionAdapter extends GuardStateInstructionAdapter implements MethodVisitorWithAnalysisFrames {

	protected int guardStateLoc;
	protected int tmpLoc;
	protected int indexLoc;
	protected int arrayLoc;
	protected int valueLoc; // leave 2 words...

	protected ArrayShadowFrame currentArrayFrame = new ArrayShadowFrame(1,1);

	protected boolean atStartOfMethod;
	private boolean inInstrumentationCode;

	private static final Type arrayShadowType = Type.getType(AbstractArrayState.class);
	private static final Method getStateMethod = Method.getMethod("rr.state.ShadowVar getState(int)");
	private static final Method nextDimMethod = Method.getMethod("rr.state.AbstractArrayState getShadowForNextDim(rr.state.ShadowThread, Object, int)");

	private static final Type cacheType = Type.getType(rr.state.ArrayStateCache.class);
	private static final Method cacheGetMethod = Method.getMethod("rr.state.AbstractArrayState get(Object, rr.state.ShadowThread, int)");

	private static final Type arrayMapType = Type.getType(ArrayStateFactory.class);
	private static final Method mapAllocMethod = Method.getMethod("rr.state.AbstractArrayState make(Object)");

	/* 
	 * If true, then the shadow will be created as soon as the array is created.  Benefit: fewer
	 * extra allocs when multiple threads perform concurrent first accesses to array.  Downside:
	 * you may never access that array... 
	 */
	private static final boolean PRE_ALLOC_SHADOW = false;


	public FancyArrayInstructionAdapter(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);

		guardStateLoc = context.getNextFreeVar(1);
		tmpLoc = context.getNextFreeVar(1);
		indexLoc = context.getNextFreeVar(1);
		arrayLoc = context.getNextFreeVar(1);
		valueLoc = context.getNextFreeVar(2);
	}

	protected HashMap<Integer,Integer> idMap = new HashMap<Integer,Integer>();

	protected int locForArrayShadow(int id) {
		Assert.assertTrue(id >= 0);
		Integer i = idMap.get(id);
		if (i == null) {
			i = context.getNextFreeVar(1);
			idMap.put(id, i);
		}
		return i;
	}



	@Override
	public void visitCode() {
		this.atStartOfMethod = true;
		super.visitCode();
	}

	@Override
	public void visitMaxs(int stack, int vars) {
		super.visitMaxs(stack + 4, Math.max(vars, context.getMaxVar()));
	}

	protected void loadShadowForArray(ArrayShadowValue v) {
		boolean shadowInVar = currentArrayFrame.getArrayShadow(v);

		// index target
		if (!shadowInVar) {
			// index target
			this.visitInsn(DUP2);
			// index target index arry
			this.pop();
			// target index target
			this.putArrayShadowIntoCacheVar(v);
			// target index target 
			this.pop();
			// index target
		}  else {
			//Util.logf("Already loaded Shadow: id %2d.  line %d", v.id, this.getFileLine());
		}

	}

	protected int storeOpcode(int arrayStoreOpcode) {
		switch (arrayStoreOpcode) {
		case DASTORE: return DSTORE;
		case LASTORE: return LSTORE;
		case AASTORE: return ASTORE;
		case BASTORE: return ISTORE;
		case CASTORE: return ISTORE;
		case FASTORE: return FSTORE;
		case IASTORE: return ISTORE;
		case SASTORE: return ISTORE;
		}
		Assert.fail("Bad opcode");
		return -1;
	}

	protected int loadOpcode(int arrayStoreOpcode) {
		switch (arrayStoreOpcode) {
		case DASTORE: return DLOAD;
		case LASTORE: return LLOAD;
		case AASTORE: return ALOAD;
		case BASTORE: return ILOAD;
		case CASTORE: return ILOAD;
		case FASTORE: return FLOAD;
		case IASTORE: return ILOAD;
		case SASTORE: return ILOAD;
		}
		Assert.fail("Bad opcode");
		return -1;
	}

	@Override
	protected void visitArrayInsn(int opcode) {
		boolean old = inInstrumentationCode;
		inInstrumentationCode = true;

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
			//				if (!InstrumentationFilter.shouldInstrument(access)) {
			//					Util.log("Skipping: " + access);
			//					super.visitArrayInsn(opcode);
			//					return;
			//				} 

			// index target
			ArrayShadowValue v = (ArrayShadowValue) currentArrayFrame.getFromTop(1);
			loadShadowForArray(v);

			boolean multiDimAccess = (opcode == AALOAD && v.getType().getDimensions() > 1);

			// index target
			super.visitVarInsn(ISTORE, this.indexLoc);
			super.visitVarInsn(ASTORE, this.arrayLoc);
			//

			if (!InstrumentationFilter.shouldInstrument(access)) {
				if (RRMain.slowMode()) Util.log("Skipping: " + access);
				//					super.visitArrayInsn(opcode);
				//						return;
			} else {

				final Label success = new Label();
				insertFastPathCode(v, success, false);

				super.visitVarInsn(ALOAD, this.arrayLoc);
				super.visitVarInsn(ILOAD, this.indexLoc);
				// index target 
				this.push(access.getId());
				// arrayAccessDataid index target 
				super.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread arrayAccessDataid index target   
				super.visitVarInsn(ALOAD, locForArrayShadow(v.id));				
				// ArrayShadow ShadowThread arrayAccessDataid index target   
				this.invokeStatic(Constants.MANAGER_TYPE, Constants.READ_ARRAY_WITH_UPDATER_METHOD);
				// 
				this.visitLabel(success);
			}
			if (multiDimAccess) {
				super.visitVarInsn(ALOAD, locForArrayShadow(v.id));
				// ArrayShadow 
				super.visitVarInsn(ALOAD, threadDataLoc);				
				// ShadowThread ArrayShadow
				super.visitVarInsn(ALOAD, this.arrayLoc);
				super.visitVarInsn(ILOAD, this.indexLoc);
				// index target ShadowThread  ArrayShadow
				super.visitArrayInsn(opcode);
				// value ShadowThread  ArrayShadow
				this.dupX2();
				// value ShadowThread ArrayShadow value 
				super.visitVarInsn(ILOAD, this.indexLoc);
				// index value ShadowThread ArrayShadow value 
				this.invokeVirtual(arrayShadowType, nextDimMethod);
				// ArrayShadow value 
				super.visitVarInsn(ASTORE, locForArrayShadow(v.tmp.id));
				// value
			} else {				
				// 
				super.visitVarInsn(ALOAD, this.arrayLoc);
				super.visitVarInsn(ILOAD, this.indexLoc);
				// index target 
				super.visitArrayInsn(opcode);
				// value
			}			
		}

		break;


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

			ArrayShadowValue v = (ArrayShadowValue) currentArrayFrame.getFromTop(2);  // remember: doubles/longs only take one stack slot -- stupid design of asm... 

			// value index target
			super.visitVarInsn(this.storeOpcode(opcode), this.valueLoc);

			loadShadowForArray(v);

			// index target
			super.visitVarInsn(ISTORE, this.indexLoc);
			super.visitVarInsn(ASTORE, this.arrayLoc);
			//

			final Label success = new Label();

			insertFastPathCode(v, success, true);

			//
			super.visitVarInsn(ALOAD, this.arrayLoc);
			super.visitVarInsn(ILOAD, this.indexLoc);
			// index target 
			this.push(access.getId());
			// arrayAccessDataid index target 
			super.visitVarInsn(ALOAD, threadDataLoc);				
			// ShadowThread arrayAccessDataid index target   
			super.visitVarInsn(ALOAD, locForArrayShadow(v.id));				
			// ArrayShadow ShadowThread arrayAccessDataid index target   
			this.invokeStatic(Constants.MANAGER_TYPE, Constants.WRITE_ARRAY_WITH_UPDATER_METHOD);
			// 
			this.visitLabel(success);

			//
			super.visitVarInsn(ALOAD, this.arrayLoc);
			super.visitVarInsn(ILOAD, this.indexLoc);
			super.visitVarInsn(this.loadOpcode(opcode), this.valueLoc);
			// value index target 
			super.visitArrayInsn(opcode);
			// 
			break;
		}
		default:
			Assert.panic("Not an target opcode!");
		}	
		inInstrumentationCode = old;
	}



	protected void insertFastPathCode(ArrayShadowValue v, final Label success, boolean isWrite) {
		if (!RR.nofastPathOption.get()) {
			// use same method as field accesses....
			int shadowLoc = locForArrayShadow(v.id);
			super.visitVarInsn(ALOAD, shadowLoc);
			// target-shadow
			super.visitVarInsn(ILOAD, this.indexLoc);
			// index target-shadow 
			this.invokeVirtual(arrayShadowType, getStateMethod);
			// ShadowVar 
			super.visitVarInsn(ASTORE, guardStateLoc);	
			// 
			ASMUtil.insertArrayFastPathCode(this, isWrite, shadowLoc, guardStateLoc, threadDataLoc, success, this.indexLoc);
		}
	}


	//
	//  Old Version that didn't have the option to use indicies.
	//	private void insertFastPathCode(ArrayShadowValue v, final Label success) {
	//		if (!RR.nofastPathOption.get()) {
	//			super.visitVarInsn(ALOAD, locForArrayShadow(v.id));				
	//			// target-shadow
	//			super.visitVarInsn(ILOAD, this.indexLoc);
	//			// index target-shadow 
	//			this.invokeVirtual(arrayShadowType, getStateMethod);
	//			// ShadowVar 
	//			super.visitVarInsn(ASTORE, guardStateLoc);	
	//			// 
	//			ASMUtil.insertFastPathCode(this, false, guardStateLoc, threadDataLoc, success);
	//			// 
	//		}
	//	}


	protected void putArrayShadowIntoCacheVar(ArrayShadowValue v) {
		if (v.id > -1) {
			if (!currentArrayFrame.getArrayShadow(v)) {
				this.forceShadowIntoCacheVar(v);
			}
		}		
	}

	protected void forceShadowIntoCacheVar(ArrayShadowValue v) {
		//	Util.logf("Loading Shadow: id %2d.  line %d", v.id, this.getFileLine());

		AbstractArrayStateCache cache = ArrayStateCache.make(this.getFileName() + ":" + this.getFileLine() + "(astore)");
		int cacheId = cache.getId();
		// target
		this.dup();
		// target target
		super.visitVarInsn(ALOAD, threadDataLoc);
		// threadData target target
		this.push(cacheId);
		// int threadData target target
		this.invokeStatic(cacheType, cacheGetMethod);
		// ArrayShadow target
		super.visitVarInsn(ASTORE, this.locForArrayShadow(v.id));
		// target
	}


	@Override
	public void visitVarInsn(int opcode, int var) { 

		if (inInstrumentationCode || var >= currentArrayFrame.getLocals()) {
			super.visitVarInsn(opcode, var);
			return;
		}

		switch (opcode) {
		case ALOAD: {
			ArrayShadowValue v = (ArrayShadowValue) currentArrayFrame.getLocal(var);
			super.visitVarInsn(opcode, var);
			// target
			this.putArrayShadowIntoCacheVar(v);
			// target
			break;
		}
		case ASTORE: {
			ArrayShadowValue v = (ArrayShadowValue) currentArrayFrame.getFromTop(0);
			if (v.id > -1) 
				// target 
				this.putArrayShadowIntoCacheVar(v);
			// target
			super.visitVarInsn(opcode, var);
			break;  
		}
		default:
			super.visitVarInsn(opcode, var);
		}
	}

	@Override
	public void visitIntInsn(int opcode, int operand) {
		switch (opcode) {
		case NEWARRAY: {
			super.visitIntInsn(opcode, operand);
			if (PRE_ALLOC_SHADOW) {
				this.dup();
				this.invokeStatic(arrayMapType, mapAllocMethod);
				this.pop();
			}
			break;
		}
		default:
			super.visitIntInsn(opcode, operand);
		}
	}

	@Override
	public void visitTypeInsn(int opcode, String desc) {
		switch (opcode) {
		case ANEWARRAY: {
			super.visitTypeInsn(opcode, desc);
			if (PRE_ALLOC_SHADOW) {
				this.dup();
				this.invokeStatic(arrayMapType, mapAllocMethod);
				this.pop();
			}
			break;
		}
		default:
			super.visitTypeInsn(opcode, desc);
		}    	
	}


	public void visitAnalysisFrame(Frame f) {
		currentArrayFrame = (ArrayShadowFrame) f;
		if (atStartOfMethod) {
			for (int i = 0; i < currentArrayFrame.getLocals(); i++) {
				ArrayShadowValue value = (ArrayShadowValue)currentArrayFrame.getLocal(i);
				if (value.id > -1) {
					Util.log("Preload Argument " + i);
					this.visitVarInsn(ALOAD, i);
					this.forceShadowIntoCacheVar(value);
					this.pop();
				}
			}
			atStartOfMethod = false;
		}
	}
}
