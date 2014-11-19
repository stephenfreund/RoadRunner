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

package rr.instrument.analysis;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.List;

import rr.org.objectweb.asm.AnnotationVisitor;
import rr.org.objectweb.asm.Attribute;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.tree.AnnotationNode;
import rr.org.objectweb.asm.tree.LocalVariableNode;
import rr.org.objectweb.asm.tree.MethodNode;
import rr.org.objectweb.asm.tree.TryCatchBlockNode;
import rr.org.objectweb.asm.tree.analysis.Analyzer;
import rr.org.objectweb.asm.tree.analysis.AnalyzerException;
import rr.org.objectweb.asm.tree.analysis.Frame;
import rr.org.objectweb.asm.tree.analysis.Interpreter;

import acme.util.Assert;
import acme.util.Util;

public abstract class AnalyzedMethodNode extends MethodNode implements MethodVisitorWithAnalysisFrames {

	protected MethodVisitor mv;
	protected final String owner;

	
	public AnalyzedMethodNode(MethodVisitor mv, String owner, int access, String name, String desc,
			String signature, String[] exceptions) {
		super(mv, access, name, desc, signature, exceptions);
		Assert.assertTrue(mv instanceof MethodVisitorWithAnalysisFrames);
		this.mv = mv;
		this.owner = owner;
	}

	@Override
	public void visitEnd() {
		mv.visitEnd();
		accept(mv);
	}

	public void visitAnalysisFrame(Frame f) {
		((MethodVisitorWithAnalysisFrames)mv).visitAnalysisFrame(f);
	}

	protected Analyzer makeAnalyzer(Interpreter interp) {
		return new Analyzer(interp);
	}

	protected abstract Interpreter makeInterpreter();

	@Override
	public void accept(final MethodVisitor mv) {
		// visits the method attributes
		int i, j, n;
		if (annotationDefault != null) {
			AnnotationVisitor av = mv.visitAnnotationDefault();
			AnnotationNode.accept(av, null, annotationDefault);
			if (av != null) {
				av.visitEnd();
			}
		}
		n = visibleAnnotations == null ? 0 : visibleAnnotations.size();
		for (i = 0; i < n; ++i) {
			AnnotationNode an = (AnnotationNode) visibleAnnotations.get(i);
			an.accept(mv.visitAnnotation(an.desc, true));
		}
		n = invisibleAnnotations == null ? 0 : invisibleAnnotations.size();
		for (i = 0; i < n; ++i) {
			AnnotationNode an = (AnnotationNode) invisibleAnnotations.get(i);
			an.accept(mv.visitAnnotation(an.desc, false));
		}
		n = visibleParameterAnnotations == null
		? 0
				: visibleParameterAnnotations.length;
		for (i = 0; i < n; ++i) {
			List l = visibleParameterAnnotations[i];
			if (l == null) {
				continue;
			}
			for (j = 0; j < l.size(); ++j) {
				AnnotationNode an = (AnnotationNode) l.get(j);
				an.accept(mv.visitParameterAnnotation(i, an.desc, true));
			}
		}
		n = invisibleParameterAnnotations == null
		? 0
				: invisibleParameterAnnotations.length;
		for (i = 0; i < n; ++i) {
			List l = invisibleParameterAnnotations[i];
			if (l == null) {
				continue;
			}
			for (j = 0; j < l.size(); ++j) {
				AnnotationNode an = (AnnotationNode) l.get(j);
				an.accept(mv.visitParameterAnnotation(i, an.desc, false));
			}
		}
		n = attrs == null ? 0 : attrs.size();
		for (i = 0; i < n; ++i) {
			mv.visitAttribute((Attribute) attrs.get(i));
		}
		// visits the method's code
		if (instructions.size() > 0) {
			//System.out.println("XXX" + mv);
			mv.visitCode();
			// visits try catch blocks
			for (i = 0; i < tryCatchBlocks.size(); ++i) {
				((TryCatchBlockNode) tryCatchBlocks.get(i)).accept(mv);
			}

			Frame frames[] = null;
			AnalyzerException exc = null;
			Analyzer analysis = makeAnalyzer(makeInterpreter());
			try {
				frames = analysis.analyze(owner, this);
			} catch (AnalyzerException e) {
				e.printStackTrace();
				exc = e;
				frames = analysis.getFrames();
			}

			if (exc != null) 
			{
				// visits instructions
				int m = instructions.size();
				Textifier tt = new Textifier();
				TraceMethodVisitor t = new TraceMethodVisitor(tt);
				for (int k = 0; k < m; k++) {
					if (frames[k] != null) {
						t.visitAnalysisFrame(frames[k]);
					}
					instructions.get(k).accept(t);
				}
				ByteArrayOutputStream out = new ByteArrayOutputStream(); 
				t.visitEnd();
				String s = "";
				int ik = 0;
				for (Object o : tt.getText()) {
					s += o + "\n";
					ik++;
				}
				Assert.panic(exc);
			}
			
			// visits instructions
			int m = instructions.size();
			for (int k = 0; k < m; k++) {
				final int opcode = instructions.get(k).getOpcode();
				
				// For debugging the instrumentor...			
				// Textifier tt = new Textifier();
				// TraceMethodVisitor t = new TraceMethodVisitor(tt);
				// if (frames[k] != null) t.visitAnalysisFrame(frames[k]);
				// instructions.get(k).accept(t);
				// Util.log(tt.getText());
				
				((MethodVisitorWithAnalysisFrames)mv).visitAnalysisFrame(frames[k]);
				instructions.get(k).accept(mv);
			}

			// visits local variables
			n = localVariables == null ? 0 : localVariables.size();
			for (i = 0; i < n; ++i) {
				((LocalVariableNode) localVariables.get(i)).accept(mv);
			}
			// visits maxs
			mv.visitMaxs(maxStack, maxLocals);
		}
		mv.visitEnd();
	}


}
