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
import rr.org.objectweb.asm.commons.Method;
import acme.util.Util;
import rr.RRMain;
import rr.instrument.Constants;
import rr.loader.RRTypeInfo;
import rr.meta.FieldAccessInfo;
import rr.meta.FieldInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.meta.OperationInfo;

public class GuardStateInstructionAdapter extends ThreadDataInstructionAdapter {

	protected static boolean instrument;

	public GuardStateInstructionAdapter(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
		instrument = true;
	}

	public void visitAccessMethod(String owner, String fName, String desc, boolean isPut, boolean isStatic, int fad, int tdLoc) {
		this.push(fad);
		this.visitVarInsn(ALOAD, this.context.getThreadDataVar());
		if (isStatic) {
			invokeStatic(Type.getObjectType(owner), Constants.getAccessMethod(owner, fName, desc, isPut));
		} else {
			if (this.getMethod().getOwner().getName().equals(owner)) {
				invokeSpecial(Type.getObjectType(owner), Constants.getAccessMethod(owner, fName, desc, isPut));
			} else {
				invokeVirtual(Type.getObjectType(owner), Constants.getAccessMethod(owner, fName, desc, isPut));
			}
		}
	}
		
	@Override
	public void visitFieldInsn(final int opcode, final String owner, final String name, final String desc) {
		if (!name.contains("$rr")) {
			FieldInfo f = RRTypeInfo.resolveFieldDescriptor(owner, name, desc);
			if (InstrumentationFilter.shouldInstrument(f)) {
				switch (opcode) {
				case GETFIELD: 
				case PUTFIELD: 				
				case GETSTATIC: 
				case PUTSTATIC: 
					boolean isWrite = opcode == PUTSTATIC || opcode == PUTFIELD;
					boolean isStatic = opcode == PUTSTATIC || opcode == GETSTATIC;
					FieldAccessInfo access = MetaDataInfoMaps.makeFieldAccess(this.getLocation(), this.getMethod(), isWrite, f);
					if (!shouldInstrument(access)) {
						if (RRMain.slowMode()) Util.log("Skipping field access: " + access);
						super.visitFieldInsn(opcode, owner, name, desc);
					} else { 
						int fad = access.getId();
						this.visitAccessMethod(owner, name, desc, isWrite, isStatic, fad, threadDataLoc);
					}
					return;
				}
			}
		} 
		super.visitFieldInsn(opcode, owner, name, desc);
	}

	protected boolean shouldInstrument(OperationInfo access) {
		return InstrumentationFilter.shouldInstrument(access) && instrument;
	}
}
