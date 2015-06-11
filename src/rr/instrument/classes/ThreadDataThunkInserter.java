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
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.JSRInlinerAdapter;
import rr.instrument.ASMUtil;
import rr.instrument.Constants;
import rr.instrument.Instrumentor;
import rr.instrument.analysis.MethodVisitorWithAnalysisFrames;
import rr.instrument.analysis.PrintingAnalyzerAdapter;
import rr.instrument.analysis.TraceMethodVisitor;
import rr.instrument.array.ArrayAnalysis;
import rr.instrument.methods.ArrayTypeExtractor;
import rr.instrument.methods.FancyArrayInstructionAdapter;
import rr.instrument.methods.GuardStateInstructionAdapter;
import rr.instrument.methods.NoOpMethodReplacer;
import rr.instrument.methods.RRMethodAdapter;
import rr.instrument.methods.ReflectionMethodReplacer;
import rr.instrument.methods.SimpleArrayInstructionAdapter;
import rr.instrument.methods.SimpleArrayWithValuesInstructionAdapter;
import rr.instrument.methods.SpecialMethodReplacer;
import rr.instrument.methods.SystemMethodReplacer;
import rr.instrument.methods.ThreadDataInstructionAdapter;
import rr.meta.ClassInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.state.ArrayStateFactory;
import rr.tool.RR;
import acme.util.Assert;
import acme.util.Debug;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

public class ThreadDataThunkInserter extends RRClassAdapter implements Opcodes {

	public static CommandLineOption<Boolean> noConstructorOption = 
		CommandLine.makeBoolean("noConstructor", false, CommandLineOption.Kind.STABLE, "Turn off instrumentation of constructors and class initializers.");
	
	protected boolean instrumentCode;

	public ThreadDataThunkInserter(final ClassVisitor cv, boolean instrumentCode) {
		super(cv);
		this.instrumentCode = instrumentCode;
	}

	@Override
	public MethodVisitor visitMethod(
			final int access,
			final String name,
			final String desc,
			final String signature,
			final String[] exceptions)
	{
		ClassInfo owner = context.getRRClass();
		MethodInfo method = MetaDataInfoMaps.getMethod(owner, name, desc);

		if (!InstrumentationFilter.shouldInstrument(method) ||
			(name.startsWith("<init>") && noConstructorOption.get())) {
			return cv.visitMethod(access, name, desc, signature, exceptions);
		} else {
			String newDesc;
			String newName;
			MethodInfo newMethod;
			final int maxVar = Instrumentor.methodContext.get(method).getMaxVar();

			if (InstrumentationFilter.supportsThreadStateParam(method)) {
				newDesc = ASMUtil.addThreadDataToDescriptor(desc);
				newName = Constants.getThreadLocalName(name);
				newMethod = MetaDataInfoMaps.getMethod(owner, newName, newDesc);
				newMethod.setFlags(method);
				Instrumentor.methodContext.get(newMethod).setFirstFreeVar(maxVar+1);
				createThreadDataThunk(access, name, desc, newDesc, signature, exceptions);
			} else {
				newDesc = desc;
				newName = name;
				newMethod = method;
			}

			MethodVisitor mv = cv.visitMethod(access, newName, newDesc, signature, exceptions);

			if (mv == null || (access & ACC_ABSTRACT) != 0) {
				return mv;
			}

			if (instrumentCode) {

				if (ArrayStateFactory.arrayOption.get() != ArrayStateFactory.ArrayMode.NONE) {
					if (RR.valuesOption.get()) {
						if (version < V1_6) {
							Yikes.yikes("Classfile is version " + version + ", but -values requires class files be at least version " + V1_6 + ".  Recompile with javac version 1.6 or higher, or crashes may result.");
						}
						
						SimpleArrayWithValuesInstructionAdapter mv2 = new SimpleArrayWithValuesInstructionAdapter(mv, newMethod);
						// MethodVisitor p = new PrintingAnalyzerAdapter(owner.getName(), access, newName, desc, mv2);
						MethodVisitor p = mv2;
						ArrayTypeExtractor mv3 = new ArrayTypeExtractor(owner.getName(), access, newName, newDesc, p);

						mv2.setTypeAnalyzer(mv3);
						mv = mv3;
						if (Debug.debugOn("analysis")) {
							Assert.fail("Analysis not supported right now.");
//							mv = new PrintingAnalyzerAdapter(owner.getName(), access, newName, newDesc, mv);
//							mv = new MethodAdapterWithAnalysisFrames(mv);
//							mv = new TraceMethodVisitorWithAnalysisFrames((MethodVisitorWithAnalysisFrames)mv, owner.getName(), newName, newDesc);
						}
					} else if (!Instrumentor.fancyOption.get()) {
						mv = new SimpleArrayInstructionAdapter(mv, newMethod);
					} else {
						mv = new FancyArrayInstructionAdapter(mv, newMethod);
				//		mv = new MethodVisitorWithAnalysisFrames(mv, access, newName, desc, signature, exceptions);
						if (Debug.debugOn("analysis")) {
							Assert.fail("Analysis not supported right now.");
//							mv = new TraceMethodVisitor();
						}
						mv = new ArrayAnalysis(mv, owner.getName(), access, newName, desc, signature, exceptions);
					}
				} else {
					mv = new GuardStateInstructionAdapter(mv, newMethod);
				}
			} else {
				mv = new ThreadDataInstructionAdapter(mv, newMethod);
			} 
			
			
			mv = new SpecialMethodReplacer(mv, newMethod);
			mv = new SystemMethodReplacer(mv, newMethod);
			mv = new NoOpMethodReplacer(mv, newMethod);
			if (Instrumentor.trackReflectionOption.get()) {
				mv = new ReflectionMethodReplacer(mv, newMethod);
			}
			mv = new JSRInlinerAdapter(mv, access, newName, newDesc, signature, exceptions);

			return mv;
		}
	}


	private void createThreadDataThunk(int access, String name, String desc, String newDesc, String signature, String[] exceptions) {
		final ClassInfo owner = context.getRRClass();
		MethodInfo method = MetaDataInfoMaps.getMethod(owner, name, desc);

		RRMethodAdapter mv = new RRMethodAdapter(cv.visitMethod(access & ~ACC_ABSTRACT, name, desc, signature, exceptions), method);

		mv.visitCode();
		if ((access & ACC_STATIC) == 0) {
			mv.visitVarInsn(ALOAD, 0);		
			Type args[] = Type.getArgumentTypes(desc);
			int localVarIndex = 1;
			for (int i = 0; i < args.length; i++) {
				mv.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
				localVarIndex += args[i].getSize();

			} 
			mv.invokeStatic(Constants.THREAD_STATE_TYPE, Constants.CURRENT_THREAD_METHOD);
			mv.visitMethodInsn(((access & Opcodes.ACC_ABSTRACT) == 0) ? INVOKESPECIAL : INVOKEVIRTUAL, owner.getName(), Constants.getThreadLocalName(name), newDesc, false);
		} else {
			Type args[] = Type.getArgumentTypes(desc);
			int localVarIndex = 0;
			for (int i = 0; i < args.length; i++) {
				mv.visitVarInsn(ASMUtil.loadInstr(args[i]), localVarIndex);
				localVarIndex += args[i].getSize();

			}
			mv.invokeStatic(Constants.THREAD_STATE_TYPE, Constants.CURRENT_THREAD_METHOD);
			mv.visitMethodInsn(INVOKESTATIC, owner.getName(), Constants.getThreadLocalName(name), newDesc, false);
		}
		mv.visitInsn(ASMUtil.returnInstr(Type.getReturnType(newDesc)));
		mv.visitMaxs(3, 2);
		mv.visitEnd();
	}
}
