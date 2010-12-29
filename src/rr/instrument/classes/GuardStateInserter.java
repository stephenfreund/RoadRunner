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

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.instrument.Instrumentor;
import rr.instrument.methods.RRMethodAdapter;
import rr.meta.ClassInfo;
import rr.meta.FieldInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.tool.RR;

public class GuardStateInserter extends RRClassAdapter implements Opcodes {

	protected boolean alreadyAddedPerObjectGuard = false;

	public GuardStateInserter(final ClassVisitor cv) {
		super(cv);
	}

	@Override
	public void visit(int version, int access,String name, String signature, String superName, String[] interfaces)	{
		super.visit(version, ASMUtil.makePublic(access), name, signature, superName, interfaces);
	}

	protected RRMethodAdapter makeGenerator(final int access, final String name, final String desc, boolean isPut) {
		final String currentClass = this.getCurrentClass().getName();
		Method m = Constants.getAccessMethod(currentClass, name, desc, isPut);
		String mName = m.getName();
		String mDesc = m.getDescriptor();
		MethodInfo method = MetaDataInfoMaps.getMethod(this.getCurrentClass(), mName, mDesc);
		method.setFlags((access & ACC_STATIC) != 0, (access & ACC_NATIVE) != 0);
		return new RRMethodAdapter(cv.visitMethod(access, mName, mDesc, null, null), method);
	}


	public void visitGetShadow(RRMethodAdapter mv, String owner, String name, boolean isStatic) {
		final Type ownerType = Type.getObjectType(owner);
		final String shadowFieldName = Constants.getShadowFieldName(owner, name, isStatic);
		if (isStatic) {
			mv.getStatic(ownerType, shadowFieldName, Constants.GUARD_STATE_TYPE);
		} else {
			mv.getField(ownerType, shadowFieldName, Constants.GUARD_STATE_TYPE);
		}
	}

	protected void addPutMethod(final int access, final String name, final String desc) {
		ClassInfo rrClass = this.getCurrentClass();
		boolean isVolatile = (access & ACC_VOLATILE) != 0;

		final RRMethodAdapter mv = makeGenerator(access | (isVolatile ? ACC_SYNCHRONIZED : 0), name, desc, true);
		final int valueSize = ASMUtil.size(desc);

		mv.visitCode();
		if ((access & ACC_FINAL) == 0) {
			final Label success = new Label();

			mv.visitVarInsn(ALOAD, 0);
			// THIS
			visitGetShadow(mv, rrClass.getName(), name, false);
			// gs
			mv.visitVarInsn(ASTORE, 5);
			// insert fast path code.
			if (!isVolatile) ASMUtil.insertFastPathCode(mv, true, 5, 2 + valueSize, success);
			// 
			mv.visitVarInsn(ALOAD, 0);
			// this
			mv.visitVarInsn(ALOAD, 5);
			// this gs
			mv.visitVarInsn(ILOAD, 1 + valueSize);
			// this gs fadid
			mv.visitVarInsn(ALOAD, 2 + valueSize);
			// this gs fadid current 

			if (RR.valuesOption.get()) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, rrClass.getName(), name, desc);
				final Type type = Type.getType(desc);
				mv.visitVarInsn(ASMUtil.loadInstr(desc), 1);
				Method m = isVolatile ? Constants.getVolatileWriteAccessWithValueMethod(type) : Constants.getWriteAccessWithValueMethod(type);
				mv.invokeStatic(Constants.MANAGER_VALUE_TYPE, m);
				if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
					mv.checkCast(type);
				}
				mv.visitVarInsn(ASMUtil.storeInstr(desc), 1);
			} else {
				mv.invokeStatic(Constants.MANAGER_TYPE, isVolatile ? Constants.getVOLATILE_WRITE_ACCESS_METHOD() : Constants.getWRITE_ACCESS_METHOD());
			}

			mv.visitLabel(success);
		}
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ASMUtil.loadInstr(desc), 1);
		mv.visitFieldInsn(PUTFIELD, rrClass.getName(), name, desc);
		mv.visitInsn(RETURN);
		mv.visitMaxs(10,10);
		mv.visitEnd();
	}


	protected void addGetMethod(final int access, final String name, final String desc) {	
		ClassInfo rrClass = this.getCurrentClass();
		boolean isVolatile = (access & ACC_VOLATILE) != 0;

		final RRMethodAdapter mv = makeGenerator(access | (isVolatile ? ACC_SYNCHRONIZED : 0), name, desc, false);

		mv.visitCode();
		if ((access & ACC_FINAL) == 0) {
			Label success = new Label();


			mv.visitVarInsn(ALOAD, 0);
			visitGetShadow(mv, rrClass.getName(), name, false);
			// gs
			mv.visitVarInsn(ASTORE, 5);

			// insert fast path code.
			if (!isVolatile) ASMUtil.insertFastPathCode(mv, false, 5, 2, success);

			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitVarInsn(ALOAD, 2);
			if (RR.valuesOption.get()) {
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, rrClass.getName(), name, desc);
				final Type type = Type.getType(desc);
				Method m = isVolatile ? Constants.getVolatileReadAccessWithValueMethod(type) : Constants.getReadAccessWithValueMethod(type);
				mv.invokeStatic(Constants.MANAGER_VALUE_TYPE, m);
				if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
					mv.checkCast(type);
				}
				mv.visitInsn(ASMUtil.returnInstr(desc));
			} else {
				mv.invokeStatic(Constants.MANAGER_TYPE,
							    isVolatile ? Constants.getVOLATILE_READ_ACCESS_METHOD() : Constants.getREAD_ACCESS_METHOD());
			}
			mv.visitLabel(success);
		}
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, rrClass.getName(), name, desc);
		mv.visitInsn(ASMUtil.returnInstr(desc));
		mv.visitMaxs(10,10); 
		mv.visitEnd();
	}


	protected void addStaticPutMethod(final int access, final String name, final String desc) {
		ClassInfo rrClass = this.getCurrentClass();
		boolean isVolatile = (access & ACC_VOLATILE) != 0;

		RRMethodAdapter mv = makeGenerator(access | (isVolatile ? ACC_SYNCHRONIZED : 0), name, desc, true);
		int valueSize = ASMUtil.size(desc);

		mv.visitCode();
		if ((access & ACC_FINAL) == 0) {

			Label success = new Label();

			visitGetShadow(mv, rrClass.getName(), name, true);
			// gs
			mv.visitVarInsn(ASTORE, 5);
			// insert fast path code.
			if (!isVolatile) ASMUtil.insertFastPathCode(mv, true, 5, 1 + valueSize, success);

			mv.visitInsn(ACONST_NULL);
			mv.visitVarInsn(ALOAD, 5);			
			mv.visitVarInsn(ILOAD, 0 + valueSize);
			mv.visitVarInsn(ALOAD, 1 + valueSize);
			if (RR.valuesOption.get()) {
				mv.visitFieldInsn(GETSTATIC, rrClass.getName(), name, desc);
				final Type type = Type.getType(desc);
				mv.visitVarInsn(ASMUtil.loadInstr(desc), 0);
				Method m = isVolatile ? Constants.getVolatileWriteAccessWithValueMethod(type) : Constants.getWriteAccessWithValueMethod(type);
				mv.invokeStatic(Constants.MANAGER_VALUE_TYPE, m);
				if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
					mv.checkCast(type);
				}
				mv.visitVarInsn(ASMUtil.storeInstr(desc), 0);
			} else {
				mv.invokeStatic(Constants.MANAGER_TYPE, isVolatile ? Constants.getVOLATILE_WRITE_ACCESS_METHOD() : Constants.getWRITE_ACCESS_METHOD());
			}
			mv.visitLabel(success);
		}
		mv.visitVarInsn(ASMUtil.loadInstr(desc), 0);
		mv.visitFieldInsn(PUTSTATIC, rrClass.getName(), name, desc);
		mv.visitInsn(RETURN);
		mv.visitMaxs(10,10); 
		mv.visitEnd();
	}

	protected void addStaticGetMethod(final int access, final String name, final String desc) {	
		ClassInfo rrClass = this.getCurrentClass();
		boolean isVolatile = (access & ACC_VOLATILE) != 0;

		RRMethodAdapter mv = makeGenerator(access | (isVolatile ? ACC_SYNCHRONIZED : 0), name, desc, false);

		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		if ((access & ACC_FINAL) == 0) {
			Label success = new Label();

			visitGetShadow(mv, rrClass.getName(), name, true);
			// gs
			mv.visitVarInsn(ASTORE, 5);

			// insert fast path code.
			if (!isVolatile) ASMUtil.insertFastPathCode(mv, false, 5, 1, success);

			mv.visitInsn(ACONST_NULL);

			mv.visitVarInsn(ALOAD, 5);
			mv.visitVarInsn(ILOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			if (RR.valuesOption.get()) {
				mv.visitFieldInsn(GETSTATIC, rrClass.getName(), name, desc);
				final Type type = Type.getType(desc);
				Method m = isVolatile ? Constants.getVolatileReadAccessWithValueMethod(type) : Constants.getReadAccessWithValueMethod(type);
				mv.invokeStatic(Constants.MANAGER_VALUE_TYPE, m);
				if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
					mv.checkCast(type);
				}
				mv.visitInsn(ASMUtil.returnInstr(desc));
			} else {	
				mv.invokeStatic(Constants.MANAGER_TYPE, isVolatile ? Constants.getVOLATILE_READ_ACCESS_METHOD() : Constants.getREAD_ACCESS_METHOD());
			}

			mv.visitLabel(success);
		}
		mv.visitFieldInsn(GETSTATIC, rrClass.getName(), name, desc);
		mv.visitInsn(ASMUtil.returnInstr(desc));
		mv.visitMaxs(10,10); // must be computed auto
		mv.visitEnd();
	}



	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		ClassInfo rrClass = this.getCurrentClass();
		final boolean isStatic = (access & ACC_STATIC) != 0;
		FieldVisitor fv = super.visitField(ASMUtil.makePublic(access), name, desc, signature, value);


		FieldInfo field = MetaDataInfoMaps.getField(rrClass, name, desc);

		if (InstrumentationFilter.shouldInstrument(field)) {

			boolean isFinal = (access & ACC_FINAL) != 0; 

			if (!isFinal) {
				final String currentClassName = rrClass.getName();
				if (Instrumentor.fieldOption.get() == Instrumentor.FieldMode.FINE) {
					cv.visitField(ASMUtil.makePublic(access | ACC_TRANSIENT), Constants.getShadowFieldName(currentClassName, name, isStatic), Constants.GUARD_STATE_TYPE.getDescriptor(), null, null);
				} else if (!this.alreadyAddedPerObjectGuard) {
					cv.visitField(ASMUtil.makePublic(ACC_STATIC | ACC_TRANSIENT), Constants.getShadowFieldName(currentClassName, "bogus", true), Constants.GUARD_STATE_TYPE.getDescriptor(), null, null);
					cv.visitField(ASMUtil.makePublic(ACC_TRANSIENT), Constants.getShadowFieldName(currentClassName, "bogus", false), Constants.GUARD_STATE_TYPE.getDescriptor(), null, null);
					this.alreadyAddedPerObjectGuard = true;
				}
			}

			final int publicAccess = ASMUtil.makePublic(access);
			if (!isStatic) {
				addPutMethod(publicAccess, name, desc);
				addGetMethod(publicAccess, name, desc);
			} else {
				addStaticPutMethod(publicAccess, name, desc);
				addStaticGetMethod(publicAccess, name, desc);
			}

		}
		return fv;
	}
}
