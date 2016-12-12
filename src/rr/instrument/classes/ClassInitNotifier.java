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
import rr.state.ShadowThread;
import rr.tool.RR;
import rr.instrument.Constants;
import rr.loader.LoaderContext;
import rr.loader.MetaDataBuilder;
import rr.meta.ClassInfo;
import rr.meta.ClassInfo.State;
import rr.meta.FieldInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataAllocator;
import rr.meta.MetaDataInfo;
import rr.meta.MetaDataInfoKeys;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MetaDataInfoVisitor;
import rr.meta.SourceLocation;
import acme.util.Assert;
import acme.util.Util;
import acme.util.count.Counter;

public class ClassInitNotifier extends RRClassAdapter {


	private ClassInfo currentClass;
	private LoaderContext loaderContext;

	static {
		try {
			LoaderContext.bootLoaderContext.getRRClass("rr/instrument/classes/ClassInitNotifier");
		} catch (ClassNotFoundException e) {
			Assert.panic(e);
		}
	}

	private class ClassInitVisitor extends ClassAccessVisitor {

		public ClassInitVisitor(MethodVisitor mv) {
			super(mv);
		}

		@Override
		public void visitInsn(int opcode) {
			if (opcode == Opcodes.RETURN) {
				super.visitLdcInsn(currentClass.getKey());
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/ClassInitNotifier", "__$rr_init", "(Ljava/lang/String;)V", false);
			} 
			super.visitInsn(opcode);
		}
	}

	private class ClassAccessVisitor extends MethodVisitor {

		public ClassAccessVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}


		@Override
		public void visitFieldInsn(int opcode, String owner, String name,
				String desc) {
			

			super.visitFieldInsn(opcode, owner, name, desc);
			
			// only get because put -> not final, so normal
			// and do this *after* the access, because otherwise the initializer may not be
			//   finished.
			if (opcode == Opcodes.GETSTATIC) {
				ClassInfo class1 = MetaDataBuilder.preLoad(owner);
				super.visitLdcInsn(ClassInitNotifier.getClass(owner).id);
				super.visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/ClassInitNotifier", "__$rr_static_access", "(I)V", false);
			}
		} 

	}


	public ClassInitNotifier(ClassInfo currentClass, LoaderContext loader, ClassVisitor cv) {
		super(cv);
		this.loaderContext = loader;
		this.currentClass = currentClass;
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (!name.equals("<clinit>")) {
			return new ClassAccessVisitor(super.visitMethod(access, name, desc, signature, exceptions));
		} else {
			return new ClassInitVisitor(super.visitMethod(access, name, desc, signature, exceptions));
		}
	}

	public static final void __$rr_init(String className) {
		try {
			rr.tool.RREventGenerator.classInitEvent(className);
		} catch (Exception e) {
			Assert.panic(e);
		}
	}

	private static class StaticInitInfo extends MetaDataInfo {
		private final boolean done[] = new boolean[RR.maxTidOption.get()];
		private final ClassInfo c;
		final int id;
		public StaticInitInfo (int id, String name) {
			super(id, SourceLocation.NULL);
			c = MetaDataInfoMaps.getClass(name);
			this.id = id;
		}
		@Override
		protected String computeKey() {
			return c.getKey();
		}
		@Override
		public void accept(MetaDataInfoVisitor v) {
			// TODO Auto-generated method stub

		}
	}
	private static final MetaDataAllocator<StaticInitInfo> classes = new MetaDataAllocator<StaticInitInfo>(new StaticInitInfo[0]);

	public static StaticInitInfo getClass(String className) {		
		StaticInitInfo x = classes.get(MetaDataInfoKeys.getClassKey(className));
		if (x == null) {
			x = new StaticInitInfo(classes.size(), className);
			classes.put(x);
		} 
		return x;
	}


	public static final void __$rr_static_access(int id) {
		StaticInitInfo info = classes.get(id);
		ShadowThread td = ShadowThread.getCurrentShadowThread();
		int tid = td.getTid();
		if (!info.done[tid]) {
			info.done[tid] = true;
			rr.tool.RREventGenerator.classAccessEvent(info.c, td);
		}
	}

}
