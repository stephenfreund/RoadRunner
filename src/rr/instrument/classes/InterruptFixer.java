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

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;

import rr.loader.LoaderContext;
import rr.org.objectweb.asm.Label;
import acme.util.Assert;
import acme.util.count.Counter;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Generate interrupted events when a Thread gets interrupted.
 */
public class InterruptFixer extends RRClassAdapter implements Opcodes {

	public static final CommandLineOption<Boolean> noInterruptOption = CommandLine.makeBoolean("noInterrupt", false, CommandLineOption.Kind.EXPERIMENTAL, "turn off special handling of interrupt");

	static private final List<String> types = Arrays.asList(new String[] { "java/lang/InterruptedException", "java/lang/Exception", "java/lang/Throwable" });

	private class InterruptMethodAdapter extends MethodVisitor {

		protected Vector<Label> handlers = new Vector<Label>();

		public InterruptMethodAdapter(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			if (type != null && types.contains(type)) {
				handlers.add(handler);
			}

			super.visitTryCatchBlock(start, end, handler, type);
		}

		@Override
		public void visitLabel(Label label) {
			super.visitLabel(label);
			if (!noInterruptOption.get() && handlers.contains(label)) {
				try {
					LoaderContext.bootLoaderContext.getRRClass("rr/instrument/classes/InterruptFixer");
				} catch (ClassNotFoundException e) {
					Assert.panic(e);
				}
				visitInsn(DUP);
				visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/InterruptFixer", "__$rr_handleException", "(Ljava/lang/Throwable;)V", false);
			} 
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc, boolean isInterface) {
			if (!noInterruptOption.get() && owner.equals("java/lang/Thread")) {
				if (opcode == Opcodes.INVOKEVIRTUAL && name.equals("isInterrupted") && desc.equals("()Z")) {
					try {
						LoaderContext.bootLoaderContext.getRRClass("rr/instrument/classes/InterruptFixer");
					} catch (ClassNotFoundException e) {
						Assert.panic(e);
					}
					visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/InterruptFixer", "__$rr_handleIsInterrupted", "(Ljava/lang/Thread;)Z", false);
				} else if (opcode == Opcodes.INVOKESTATIC && name.equals("interrupted") && desc.equals("()Z")) {
					try {
						LoaderContext.bootLoaderContext.getRRClass("rr/instrument/classes/InterruptFixer");
					} catch (ClassNotFoundException e) {
						Assert.panic(e);
					}
					visitMethodInsn(Opcodes.INVOKESTATIC, "rr/instrument/classes/InterruptFixer", "__$rr_handleInterrupted", "()Z", false);
				} else {
					super.visitMethodInsn(opcode, owner, name, desc, isInterface);
				}
			} else {
				super.visitMethodInsn(opcode, owner, name, desc, isInterface);
			}

		}



	}

	public InterruptFixer(ClassVisitor cv) {
		super(cv);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		return new InterruptMethodAdapter(super.visitMethod(access, name, desc, signature, exceptions));
	}

	private static Counter handlerCount = new Counter("Handler", "Count");

	public static final void __$rr_handleException(Throwable o) {
		if (o instanceof InterruptedException) {
			rr.tool.RREventGenerator.interruptEvent(o);
		} else {
		}
	}

	public static final boolean __$rr_handleIsInterrupted(Thread t) {
		boolean b = t.isInterrupted();
		if (b) {
			rr.tool.RREventGenerator.interruptEvent(null);			
		}
		return b;
	}
	

	public static final boolean __$rr_handleInterrupted() {
		boolean b = Thread.interrupted();
		if (b) {
			rr.tool.RREventGenerator.interruptEvent(null);			
		}
		return b;
	}


}
