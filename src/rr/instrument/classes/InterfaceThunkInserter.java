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

import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.instrument.methods.RRMethodAdapter;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;

public class InterfaceThunkInserter extends RRClassAdapter implements Opcodes {

	public InterfaceThunkInserter(final ClassVisitor cv) {
		super(cv);
	}

	@Override
	public MethodVisitor visitMethod(
			final int access,
			final String name,
			final String desc,
			final String signature,
			final String[] exceptions)
	{
		MethodInfo method = MetaDataInfoMaps.getMethod(context.getRRClass(), name, desc);
		if (!InstrumentationFilter.shouldInstrument(method) ||
			name.startsWith("<") && ThreadDataThunkInserter.noConstructorOption.get()) {
			return super.visitMethod(access,
					name,
					desc,
					signature,
					exceptions);
		} else {
			String newDesc = ASMUtil.addThreadDataToDescriptor(desc);
			createThreadDataThunk(access, name, desc, signature, exceptions);

			String tlVersionName = Constants.getThreadLocalName(name);

			return super.visitMethod(access,
					tlVersionName,
					newDesc,
					signature,
					exceptions);
		}
	}


	private void createThreadDataThunk(int access, String name, String desc, String signature, String[] exceptions) {
		MethodInfo method = MetaDataInfoMaps.getMethod(context.getRRClass(), name, desc);
		method.setFlags((access & Opcodes.ACC_STATIC) != 0,(access & Opcodes.ACC_NATIVE) != 0, (access & ACC_SYNCHRONIZED) != 0);

		RRMethodAdapter mv = new RRMethodAdapter(cv.visitMethod(access, name, desc, signature, exceptions), method); 

		mv.visitCode();
		mv.visitMaxs(1,1);
		mv.visitEnd();

	}
}
