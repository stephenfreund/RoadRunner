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
import rr.instrument.Constants;
import rr.instrument.methods.RRMethodAdapter;
import rr.instrument.tools.ArrayFilterTool;
import rr.loader.MethodResolutionException;
import rr.loader.RRTypeInfo;
import rr.meta.ClassInfo;
import rr.meta.MetaDataInfoKeys;
import rr.meta.MethodInfo;
import rr.meta.SourceLocation;
import rr.org.objectweb.asm.Label;
import rr.state.ArrayStateFactory;

import acme.util.Assert;
import acme.util.StringMatchResult;
import acme.util.StringMatcher;
import acme.util.Util;
import acme.util.identityhash.ConcurrentIdentityHashMap;
import acme.util.identityhash.WeakIdentityHashMap;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public class ArrayAllocSiteTracker extends RRClassAdapter {
	
//	public static final ConcurrentIdentityHashMap<Object, SourceLocation> allocSites = new ConcurrentIdentityHashMap<Object,SourceLocation>();
	private static final WeakIdentityHashMap<Object, ArrayAllocSourceLocation> allocSites = new WeakIdentityHashMap<Object,ArrayAllocSourceLocation>();

	private ClassInfo currentClass;

	private class ArrayAllocSiteVisitor extends RRMethodAdapter {

		public ArrayAllocSiteVisitor(MethodVisitor mv, MethodInfo m) {
			super(mv, m);
		}


		@Override
		public void visitLineNumber(int line, Label start) {
			// TODO Auto-generated method stub
			super.visitLineNumber(line, start);
		}

		public void newArray() {
			final SourceLocation loc = new SourceLocation(this.getFileName(), this.getFileLine(), this.getByteCodeIndex());
			if (ArrayStateFactory.arrayOption.get() != ArrayStateFactory.ArrayMode.NONE && ArrayFilterTool.arrayAllocsToWatch.get().test(loc.getKey()) == StringMatchResult.ACCEPT) {
				super.visitInsn(Opcodes.DUP);
				super.visitLdcInsn(this.getByteCodeIndex());
				MethodInfo m = this.getMethod();
				String s = MetaDataInfoKeys.getMethodKey(m.getOwner(), Constants.recoverOriginalNameFromMangled(m.getName()), m.getDescriptor());
				super.visitLdcInsn(s);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/ArrayAllocSiteTracker", "__$rr_array", "(Ljava/lang/Object;ILjava/lang/String;)V", false);
			} else {
//				Util.log("Skipping array allocs at " + loc);
			}
		}


		@Override
		public void visitIntInsn(int opcode, int operand) {
			super.visitIntInsn(opcode, operand);
			if (opcode == Opcodes.NEWARRAY) {
				newArray();
			}
		}		

		@Override
		public void visitTypeInsn(int opcode, String desc) {
			super.visitTypeInsn(opcode, desc);
			if (opcode == Opcodes.ANEWARRAY) {
				newArray();
			}
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			super.visitMultiANewArrayInsn(desc, dims);
			newArray();
		}
	}

	public ArrayAllocSiteTracker(ClassInfo currentClass, ClassVisitor cv) {
		super(cv);
		this.currentClass = currentClass;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		try {
			MethodInfo m = RRTypeInfo.resolveMethodDescriptor(currentClass, name, desc);
			return new ArrayAllocSiteVisitor(super.visitMethod(access, name, desc, signature, exceptions), m);
		} catch (MethodResolutionException e) {
			Assert.warn(e);
			return super.visitMethod(access, name, desc, signature, exceptions);
		}
	}
	
	public static ArrayAllocSourceLocation get(Object o) {
		synchronized(allocSites) {
			return allocSites.get(o);
		}
	}
	
	public static class ArrayAllocSourceLocation {
		final private int dimension;
		final private String methodKey;
		final private int bci;

		public ArrayAllocSourceLocation(String methodKey, int bci, int dim) {
			this.dimension = dim;
			this.bci = bci;
			this.methodKey = methodKey;
		}

		public String getMethodKey() {
			return methodKey;
		}

		public int getDimension() {
			return dimension;
		}
		
		public int getBci() {
			return bci;
		}

		public String toString() {
			return  methodKey + ":" + bci + ":" + getDimension();
		}

	}
	// this version takes the dimension too.
	public static final void __$rr_array(Object o, int byteCodeIndex, String methodKey, int dimension) {
		try {
			final ArrayAllocSourceLocation loc = new ArrayAllocSourceLocation(methodKey, byteCodeIndex, dimension);
			synchronized(allocSites) { allocSites.put(o, loc); }

			if (o instanceof Object[]) {
				Object[] a = (Object[])o;
				for (Object aa : a) {
					if (aa != null && aa.getClass().isArray()) {
						__$rr_array(aa, byteCodeIndex, methodKey, dimension + 1);
					}
				}
			}

		} catch (Exception e) {
			Assert.panic(e);
		}
	}

	
	public static final void __$rr_array(Object o, int byteCodeIndex, String methodKey) {
		__$rr_array(o, byteCodeIndex, methodKey, 0);
	}

}
