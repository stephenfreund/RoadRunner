///******************************************************************************
//
//Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
//                    and Stephen Freund (Williams College) 
//
//All rights reserved.  
//
//Redistribution and use in source and binary forms, with or without
//modification, are permitted provided that the following conditions are
//met:
//
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//
//    * Redistributions in binary form must reproduce the above
//      copyright notice, this list of conditions and the following
//      disclaimer in the documentation and/or other materials provided
//      with the distribution.
//
//    * Neither the names of the University of California, Santa Cruz
//      and Williams College nor the names of its contributors may be
//      used to endorse or promote products derived from this software
//      without specific prior written permission.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
//"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
//LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
//A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
//HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
//LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
//DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
//THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
//(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//******************************************************************************/
//
//// based on ASM code
//
///***
// * ASM: a very small and fast Java bytecode manipulation framework
// * Copyright (c) 2000-2005 INRIA, France Telecom
// * All rights reserved.  
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions
// * are met:
// * 1. Redistributions of source code must retain the above copyright
// *    notice, this list of conditions and the following disclaimer.
// * 2. Redistributions in binary form must reproduce the above copyright
// *    notice, this list of conditions and the following disclaimer in the
// *    documentation and/or other materials provided with the distribution.
// * 3. Neither the name of the copyright holders nor the names of its
// *    contributors may be used to endorse or promote products derived from
// *    this software without specific prior written permission.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// * THE POSSIBILITY OF SUCH DAMAGE.
// */
//package rr.instrument.tools;
//
//import java.io.FileInputStream;
//import java.util.List;
//
//import rr.org.objectweb.asm.ClassReader;
//import rr.org.objectweb.asm.ClassVisitor;
//import rr.org.objectweb.asm.Type;
//import rr.org.objectweb.asm.tree.ClassNode;
//import rr.org.objectweb.asm.tree.MethodNode;
//import rr.org.objectweb.asm.tree.TryCatchBlockNode;
//import rr.org.objectweb.asm.tree.analysis.Analyzer;
//import rr.org.objectweb.asm.tree.analysis.Frame;
//import rr.org.objectweb.asm.tree.analysis.SimpleVerifier;
//import rr.org.objectweb.asm.util.CheckClassAdapter;
//import rr.org.objectweb.asm.util.TraceMethodVisitor;
//
//import acme.util.Util;
//
//
///**
// * A {@link ClassAdapter} that checks that its methods are properly used. More
// * precisely this class adapter checks each method call individually, based
// * <i>only</i> on its arguments, but does <i>not</i> check the <i>sequence</i>
// * of method calls. For example, the invalid sequence
// * <tt>visitField(ACC_PUBLIC, "i", "I", null)</tt> <tt>visitField(ACC_PUBLIC,
// * "i", "D", null)</tt>
// * will <i>not</i> be detected by this class adapter.
// * 
// * @author Eric Bruneton
// */
//public class RRVerifier extends CheckClassAdapter {
//
//	/**
//	 * Checks a given class. <p> Usage: RRVerifier &lt;fully qualified
//	 * class name or class file name&gt;
//	 * 
//	 * @param args the command line arguments.
//	 * 
//	 * @throws Exception if the class cannot be found, or if an IO exception
//	 *         occurs.
//	 */
//	public static void main(final String[] args) throws Exception {
//		if (args.length != 1) {
//			Util.log("Verifies the given class.");
//			Util.log("Usage: RRVerifier "
//					+ "<fully qualified class name or class file name>");
//			return;
//		}
//		ClassReader cr;
//		if (args[0].endsWith(".class")) {
//			cr = new ClassReader(new FileInputStream(args[0]));
//		} else {
//			cr = new ClassReader(args[0]);
//		}
//
//		verify(cr, false, RRVerifier.class.getClassLoader());
//	}
//
//
//    /**
//     * Checks a given class
//     * 
//     * @param cr a <code>ClassReader</code> that contains bytecode for the
//     *        analysis.
//     * @param dump true if bytecode should be printed out not only when errors
//     *        are found.
//     * @param pw write where results going to be printed
//     */
//    public static void verify(
//        final ClassReader cr,
//        final boolean dump,
//        final ClassLoader loader)
//    {
//        ClassNode cn = new ClassNode();
//        cr.accept(new RRVerifier(cn), ClassReader.SKIP_DEBUG);
//
//        List methods = cn.methods;
//        for (int i = 0; i < methods.size(); ++i) {
//            MethodNode method = (MethodNode) methods.get(i);
//			Analyzer a = new Analyzer(new SimpleVerifier(Type.getObjectType(cn.name),
//					Type.getObjectType(cn.superName),
//					false) {
//				@Override
//				protected Class getClass(final Type t) {
//					try {
//						if (t.getSort() == Type.ARRAY) {
//							return Class.forName(t.getDescriptor().replace('/', '.'), false, loader);
//						}
//						return Class.forName(t.getClassName(), false, loader);
//					} catch (ClassNotFoundException e) {
//						throw new RuntimeException(e.toString());
//					}
//				}
//			});
//
//            
//            
//            try {
//                a.analyze(cn.name, method);
//                if (!dump) {
//                    continue;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            
//            Frame[] frames = a.getFrames();
//
//            TraceMethodVisitor mv = new TraceMethodVisitor();
//
//            Util.log(method.name + method.desc);
//            String st = "";
//            for (int j = 0; j < method.instructions.size(); ++j) {
//                method.instructions.get(j).accept(mv);
//
//                StringBuffer s = new StringBuffer();
//                Frame f = frames[j];
//                if (f == null) {
//                    s.append('?');
//                } else {
//                    for (int k = 0; k < f.getLocals(); ++k) {
//                        s.append(getShortName(f.getLocal(k).toString()))
//                                .append(' ');
//                    }
//                    s.append(" : ");
//                    for (int k = 0; k < f.getStackSize(); ++k) {
//                        s.append(getShortName(f.getStack(k).toString()))
//                                .append(' ');
//                    }
//                }
//                while (s.length() < method.maxStack + method.maxLocals + 1) {
//                    s.append(' ');
//                }
//                st += (Integer.toString(j + 100000).substring(1));
//                st += (" " + s + " : " + mv.buf); // mv.text.get(j));
//            }
//            for (int j = 0; j < method.tryCatchBlocks.size(); ++j) {
//                ((TryCatchBlockNode) method.tryCatchBlocks.get(j)).accept(mv);
//                st += (" " + mv.buf);
//            }
//            Util.log(st);
//        }
//    }
//
//    private static String getShortName(final String name) {
//    	if (name.startsWith("[")) {
//    		return "[" + getShortName(name.substring(1));
//    	}
//        int n = name.lastIndexOf('/');
//        int k = name.length();
//        if (name.charAt(k - 1) == ';') {
//            k--;
//        }
//        return n == -1 ? name : name.substring(n + 1, k);
//    }
//
//	
//	/**
//	 * Constructs a new {@link RRVerifier}.
//	 * 
//	 * @param cv the class visitor to which this adapter must delegate calls.
//	 */
//	public RRVerifier(final ClassVisitor cv) {
//		super(cv);
//	}
//
//	@Override
//	public void visit(
//			final int version,
//			final int access,
//			final String name,
//			final String signature,
//			final String superName,
//			final String[] interfaces) {
//		Util.logf("Verifying %s...", name);
//		super.visit(version, access, name, signature, superName, interfaces);
//	}
//}
