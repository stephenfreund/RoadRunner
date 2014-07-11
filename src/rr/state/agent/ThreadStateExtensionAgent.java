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

package rr.state.agent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;

import rr.org.objectweb.asm.ClassReader;
import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.tool.ToolLoader;
import rr.loader.InstrumentingDefineClassLoader;
import rr.loader.NonInstrumentingPreDefineClassLoader;
import rr.loader.RepositoryBuildingDefineClassLoader;
import acme.util.Assert;
import acme.util.Util;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;
import acme.util.time.TimedStmt;

public class ThreadStateExtensionAgent {

	public enum InstrumentationMode { INST, REP, NOINST };
	

	private static final String FIELD_ACCESSOR_NAME_PREFIX = "ts_get";

	static private final StateExtensionTransformer trans = new StateExtensionTransformer();

	public static CommandLineOption<Boolean> noDecorationInline = 
		CommandLine.makeBoolean("noDecInline", false, CommandLineOption.Kind.EXPERIMENTAL, "Turn off Thread State Decoration Inlining.");


	public static void premain(String agentArgs, Instrumentation inst) {
		System.err.println("[premain: Installling RoadRunner Agent...]");
		inst.addTransformer(trans);
		Util.log("RoadRunner Agent Loaded.");
	}

	public synchronized static void addInstrumenter(InstrumentationMode mode) {
		DefineClassListener hook = null; 
		switch (mode) {
			case INST: hook = new InstrumentingDefineClassLoader(); break;
			case NOINST: hook = new NonInstrumentingPreDefineClassLoader(); break;
			case REP: hook = new RepositoryBuildingDefineClassLoader(); break; 
		}
		Util.log("Installing DefineClassListener " + hook);
		trans.setDefineClassHook(hook);
	}



	private static class ToolClassVisitor extends ClassVisitor implements Opcodes {

		String owner;
		private ToolLoader loader;

		public ToolClassVisitor(ToolLoader loader, ClassVisitor cv, String owner) {
			super(ASM5, cv);
			this.owner = owner;
			this.loader = loader;
		}

		@Override
		public void visit(int version, int access, String name,
				String signature, String superName, String[] interfaces) {
			if (!superName.equals("rr/tool/Tool")) {
				loader.prepToolClass(superName);
			}
			super.visit(version, access, name, signature, superName, interfaces);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			if (name.startsWith(FIELD_ACCESSOR_NAME_PREFIX)) {
				ThreadStateFieldExtension f = new ThreadStateFieldExtension(owner, "rr/state/ShadowThread", name.substring(7), Type.getReturnType(desc).getDescriptor());
				trans.addField(f);
			}
			return super.visitMethod(access, name, desc, signature, exceptions);
		}

	}


	public static void registerTool(final ToolLoader loader, final String name, final InputStream in) {
		if (noDecorationInline.get()) { 
			Util.log("Skipping ShadowThread extension for " + name);
			return;
		}

		trans.addToolClassToWatchList(name);
		try {
			Util.log(new TimedStmt("Extending States for " + name) {
				@Override
				public void run() throws Exception {
					ClassReader cr;
					cr = new ClassReader(in);
					ClassVisitor cv = new ToolClassVisitor(loader, new ClassVisitor(Opcodes.ASM5) { }, name);
					cr.accept(cv, 0);
				}
			});	
		} catch (IOException e) {
			e.printStackTrace();
			Assert.panic("Cant read class file for tool '%s', IOException '%s'", name, e.toString());
		} catch (Exception e) {
			Assert.panic(e);
		} 
	}
}
