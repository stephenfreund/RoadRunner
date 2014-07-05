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

package rr.instrument.pragma;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

import rr.instrument.Constants;
import rr.instrument.hooks.SpecialMethods;
import rr.instrument.methods.RRMethodAdapter;
import rr.loader.LoaderContext;
import rr.loader.MethodResolutionException;
import rr.loader.RRTypeInfo;
import rr.meta.ClassInfo;
import rr.meta.InterruptInfo;
import rr.meta.JoinInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.meta.SourceLocation;
import rr.meta.StartInfo;
import rr.meta.WaitInfo;
import rr.org.objectweb.asm.AnnotationVisitor;
import rr.org.objectweb.asm.Attribute;
import rr.org.objectweb.asm.Label;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.Method;
import acme.util.Assert;
import acme.util.Util;

public class PragmaProcessor extends RRMethodAdapter implements Opcodes {

	protected static Vector<PragmaHandler> handlers = new Vector<PragmaHandler>();
	
	public PragmaProcessor(final MethodVisitor mv, MethodInfo m) {
		super(mv, m);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		boolean done = false;
		if (opcode == Opcodes.INVOKESTATIC) {
			for (PragmaHandler p : handlers) {
				done = p.process(owner,  name,  desc) || done;
			}
		}
		if (!done) {
			super.visitMethodInsn(opcode, owner, name, desc, itf);
		}
	}
	
	public static void addHandler(PragmaHandler handler) {
		handlers.add(handler);
	}

}
