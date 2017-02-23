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

package rr.instrument;

import java.io.File;
import java.util.Set;
import java.util.Vector;

import rr.org.objectweb.asm.ClassReader;
import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.ClassWriter;
import rr.org.objectweb.asm.Opcodes;
import rr.instrument.classes.AbstractOrphanFixer;
import rr.instrument.classes.ArrayAllocSiteTracker;
import rr.instrument.classes.ClassInitNotifier;
import rr.instrument.classes.CloneFixer;
import rr.instrument.classes.GuardStateInserter;
import rr.instrument.classes.InterfaceThunkInserter;
import rr.instrument.classes.InterruptFixer;
import rr.instrument.classes.JVMVersionNumberFixer;
import rr.instrument.classes.NativeMethodSanityChecker;
import rr.instrument.classes.SyncAndMethodThunkInserter;
import rr.instrument.classes.ThreadDataThunkInserter;
import rr.instrument.classes.ToolSpecificClassVisitorFactory;
import rr.instrument.noinst.NoInstSanityChecker;
import rr.loader.LoaderContext;
import rr.meta.ClassInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import acme.util.Assert;
import acme.util.count.Timer;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;
import acme.util.option.Option;

public class Instrumentor {

	public static final CommandLineOption<String> dumpClassOption  = 
			CommandLine.makeString("dump", "", CommandLineOption.Kind.STABLE, "Specifies to directory to which all metadata and instrumented class files should be dumped.  Empty string turns off dumping.", new Runnable() {
				public void run() {
					new File(dumpClassOption.get()).mkdirs();
				}
			});

	public static enum FieldMode { FINE, COARSE };

	public static final CommandLineOption<FieldMode> fieldOption = 
			CommandLine.makeEnumChoice("field", FieldMode.FINE, CommandLineOption.Kind.STABLE, "Specify granularity of shadow for objects.  FINE is one location per field.  COARSE is one location per object.", FieldMode.class);

	public static final CommandLineOption<Boolean> fancyOption = 
			CommandLine.makeBoolean("fancy", false, CommandLineOption.Kind.EXPERIMENTAL, "Use a more complex instrumentor with some untested or experimental features.  The fancy version may yield faster code.");

	public static final CommandLineOption<Boolean> verifyOption = 
			CommandLine.makeBoolean("verify", false, CommandLineOption.Kind.EXPERIMENTAL, "Verify the instrumented class files.  (Used to debug instrumentor.)");

	public static final CommandLineOption<Boolean> trackReflectionOption = 
			CommandLine.makeBoolean("trackReflection", false, CommandLineOption.Kind.EXPERIMENTAL, "Instrument calls to reflection methods that access fields/arrays to generate events.");

	public static final CommandLineOption<Boolean> trackArraySitesOption = 
			CommandLine.makeBoolean("arraySites", false, CommandLineOption.Kind.STABLE, "Track arrays only on given line locations.");

	public static final Option<Boolean> useTestAcquireOption = new Option<Boolean>("Use TestAcquires", false);

	private static final Timer insTime = new Timer("Time", "Instrumenter");

	public static final Decoration<ClassInfo,ClassContext> classContext = 
			MetaDataInfoMaps.getClasses().makeDecoration("class instrument context", DecorationFactory.Type.SINGLE, new DefaultValue<ClassInfo, ClassContext>() { 
				public ClassContext get(ClassInfo rrClass) { 
					return new ClassContext(rrClass);
				}
			});

	public static final Decoration<MethodInfo,MethodContext> methodContext = 
			MetaDataInfoMaps.getMethods().makeDecoration("method instrument context", DecorationFactory.Type.SINGLE, new DefaultValue<MethodInfo, MethodContext>() { 
				public MethodContext get(MethodInfo m) { 
					return new MethodContext(m);
				}
			});


	public static synchronized ClassWriter instrument(final LoaderContext loader, ClassReader cr) {
		long start = insTime.start();

		try { 

			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS) {
				@Override
				protected String getCommonSuperClass(final String type1, final String type2) 
				{
					try {
						ClassInfo c1 = loader.getRRClass(type1);
						ClassInfo c2 = loader.getRRClass(type2);
						Set<ClassInfo> c1Supers = c1.getSuperTypes();
						Set<ClassInfo> c2Supers = c2.getSuperTypes();
						if (c1Supers.contains(c2)) {
							return type2;
						} else if (c2Supers.contains(c1)) {
							return type1;
						}
						if (!c1.isClass() || !c2.isClass()) {
							return "java/lang/Object";
						}
						while (true) {
							c1 = c1.getSuperClass();
							if (c2Supers.contains(c1)) {
								return c1.getName();
							}
						}
					} catch (ClassNotFoundException e) {
						Assert.fail(e);
						return null;
					}

				}
			};

			// This is the "default" guess at source file name if we can't 
			// extract it from the class file.
			String fileName = cr.getClassName();
			ClassInfo currentClass = MetaDataInfoMaps.getClass(fileName);
			if (fileName.contains("$")) {
				fileName = fileName.substring(0, fileName.indexOf("$"));
			}
			fileName += ".java";
			final ClassContext ctxt = classContext.get(currentClass);
			ctxt.setFileName(fileName);

			// This visitor will attempt to record the source file name.
			final ClassVisitor cv0 = new ClassVisitor(Opcodes.ASM5, cw) {
				private String pack;
				public void visit(
						int version,
						int access,
						String name,
						String signature,
						String superName,
						String[] interfaces) {
					if (!name.contains("/")) {
						pack = "";
					} else {
						pack = name.substring(0, name.lastIndexOf("/")+1);
					}
					super.visit(version, access, name, signature, superName, interfaces);

				}

				public void visitSource(final String source, final String debug) {
					ctxt.setFileName(pack + source);
					super.visitSource(source, debug);
				}
			};



			if ((cr.getAccess() & (Opcodes.ACC_INTERFACE)) == 0) {

				ClassVisitor cv1 = new NativeMethodSanityChecker(cv0);
				cv1 = new GuardStateInserter(cv1);
				cv1 = new InterruptFixer(cv1);
				cv1 = new CloneFixer(cv1);
				cv1 = new ClassInitNotifier(currentClass, loader, cv1);
				if (trackArraySitesOption.get()) {
					cv1 = new ArrayAllocSiteTracker(currentClass, cv1);
				}
				cv1 = new AbstractOrphanFixer(cv1);
				
				// do here instead of below at (*), so that we have the proper context for the thunks...
				cv1 = insertToolSpecificVisitors(cv1);
				ClassVisitor cv2 = new ThreadDataThunkInserter(cv1, true);
				ClassVisitor cv2forThunks = new ThreadDataThunkInserter(cv1, false);
				ClassVisitor cv = new SyncAndMethodThunkInserter(cv2, cv2forThunks);

				// (*) cv = insertToolSpecificVisitors(cv); 
				
				
				cv = new JVMVersionNumberFixer(cv);
				
				cr.accept(cv, ClassReader.EXPAND_FRAMES);
			} else {			
				ClassVisitor cv = new InterfaceThunkInserter(cv0);

				cv = insertToolSpecificVisitors(cv);

				cr.accept(cv, 0);
			}
			return cw;
		} catch (Throwable e) {
			Assert.panic(e);
			return null;
		}	finally {
			insTime.stop(start);
		}
	}



	public static synchronized void sanityCheck(LoaderContext loaderContext,
			ClassReader classReader) {
		long start = insTime.start();
		try { 
			ClassVisitor cv = new NoInstSanityChecker();
			classReader.accept(cv, ClassReader.EXPAND_FRAMES);
		} catch (Throwable e) {
			Assert.panic(e);
		}	finally {
			insTime.stop(start);
		}
	}

	private static Vector<ToolSpecificClassVisitorFactory> toolVisitors = 
			new Vector<ToolSpecificClassVisitorFactory>(); 

	public static void addToolSpecificVisitor(ToolSpecificClassVisitorFactory factory) {
		toolVisitors.add(factory);
	}

	private static ClassVisitor insertToolSpecificVisitors(ClassVisitor cv) {
		for (ToolSpecificClassVisitorFactory vf : toolVisitors) {
			cv = vf.make(cv);
		}
		return cv;
	}
}



