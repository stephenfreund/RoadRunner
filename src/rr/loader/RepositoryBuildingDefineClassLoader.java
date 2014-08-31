package rr.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import rr.org.objectweb.asm.ClassReader;
import rr.org.objectweb.asm.ClassVisitor;
import rr.org.objectweb.asm.FieldVisitor;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;

import rr.instrument.Instrumentor;
import rr.org.objectweb.asm.Label;
import rr.state.agent.DefineClassListener;
import acme.util.Assert;
import acme.util.Util;
import acme.util.time.TimedStmt;

/*
 * Build a repository of all class files loaded, and all referenced from them.
 */
public class RepositoryBuildingDefineClassLoader implements DefineClassListener {

	public RepositoryBuildingDefineClassLoader() {
		String s = Instrumentor.dumpClassOption.get();
		if (s.equals("")) {
			Instrumentor.dumpClassOption.checkAndApply("rr-repository");
		}
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				loadAllNames();
			}
		});
	}

	public byte[] define(ClassLoader definingLoader, final String name, final byte[] bytes) {
		findNames(definingLoader, bytes);
		return bytes;
	}

	public void init(ClassLoader loader) {	}
	public void postDefineClass(ClassLoader definingLoader, String name) {		}


	/**********/

	static private ConcurrentHashMap<String,ClassLoader> todo = new ConcurrentHashMap<String,ClassLoader>();
	static private ClassLoader currentLoader;

	private static class EagerNameClassVisitor extends ClassVisitor {

		public EagerNameClassVisitor() {
			super(Opcodes.ASM5);
		}

		@Override
		public void visit(int version, int access, String name,
				String signature, String superName, String[] interfaces) {

			addName(name);
			if ((access & Opcodes.ACC_INTERFACE) == 0 && superName != null) {
				addName(superName);
			}
			for (String i : interfaces) {
				addName(i);
			}
			super.visit(version, access, name, signature, superName, interfaces);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc,
				String signature, Object value) {
			Type t = Type.getType(desc);
			visitType(t); 
			return super.visitField(access, name, desc, signature, value);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
				String signature, String[] exceptions) {
			visitType(Type.getReturnType(desc));
			for (Type t : Type.getArgumentTypes(desc)) {
				visitType(t);
			}
			final MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
			return new EagerNameMethodVisitor(mv);
		}

		@Override
		public void visitInnerClass(String name, String outerName,
				String innerName, int access) {
			super.visitInnerClass(name, outerName, innerName, access);
			addName(name);
		}

	}

	private static class EagerNameMethodVisitor extends MethodVisitor {

		public EagerNameMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		@Override
		public void visitFieldInsn(int opcode, String owner, String name,
				String desc) {
			addName(owner);
			visitType(Type.getType(desc));
			super.visitFieldInsn(opcode, owner, name, desc);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
				String desc, boolean isInterface) {
			addName(owner);
			visitType(Type.getReturnType(desc));
			for (Type t : Type.getArgumentTypes(desc)) {
				visitType(t);
			}
			super.visitMethodInsn(opcode, owner, name, desc,opcode == Opcodes.INVOKEINTERFACE);
		}

		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			visitType(Type.getType(desc));
			super.visitMultiANewArrayInsn(desc, dims);
		}

		@Override
		public void visitTypeInsn(int opcode, String desc) {
			visitType(Type.getObjectType(desc));
			super.visitTypeInsn(opcode, desc);
		}

		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler,
				String type) {
			if (type != null) {
				visitType(Type.getObjectType(type));
			}
			super.visitTryCatchBlock(start, end, handler, type);
		}

	}

	public static void findNames(final ClassLoader c, final byte b[]) {
		findNames(c, new ClassReader(b));
	}

	public static void findNames(final ClassLoader c, final ClassReader in) {
		currentLoader = c;
		Loader.get(c);
		EagerNameClassVisitor mcv = new EagerNameClassVisitor();
		in.accept(mcv, 0);
	}

	public static void loadAllNames() {
		final HashSet<String> copied = new HashSet<String>();
		try {
			Util.eval(new TimedStmt("Building Repository Directory") {
				@Override
				public void run() {
					boolean done = false;
					int count = 0;
					while (!done) {
						done = true;
						ConcurrentHashMap<String,ClassLoader> working = todo;
						todo = new ConcurrentHashMap<String,ClassLoader>();
						outer:
							for (Entry<String,ClassLoader> e : working.entrySet()) {
								final String name = e.getKey() + ".class";
								if (!copied.contains(name)) {
									copied.add(name);
									done = false;
									final ClassLoader loader = e.getValue();
									URL url = loader.getResource(name);

									if (url == null) {
										for (LoaderContext cl: Loader.wrappers.values()) {
											url = loader.getResource(name);
											if (url != null) break;
										}
									}

									if (url == null) {
										Util.log(" Couldn't find class file for: " + name);
									}
									try {
										findNames(loader, new ClassReader(url.openStream()));
									} catch (Exception e1) {
										Util.log(" Error recursively processing " + name);
									}
									try {
										writeToRepository("classes", name, url.openStream());
										if (count++ % 100 == 0) {
											Util.log("written: " + count);
										}
									} catch (Exception e1) {
										Util.log(" Error processing " + name);
									}

								}
							}
					}
				}
			});
		} catch (Exception e) {
			Assert.panic(e);
		}
	}

	private static final void writeToRepository(String prefix, String className, InputStream in) {
		final String dump = Instrumentor.dumpClassOption.get();
		String fullName = dump + "/" + prefix + "/" + className;
		String dirPath = fullName.substring(0,fullName.lastIndexOf("/"));
		new File(dirPath).mkdirs();
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(fullName);
			int x;
			while ((x = in.read()) != -1) {
				fos.write((byte)x);
			}
			fos.close();
		} catch (Exception e) {
			Assert.fail(e);
		} 
	}

	public static void visitType(Type t) {
		switch (t.getSort()) {
			case Type.ARRAY:
				visitType(t.getElementType());
				break;
			case Type.OBJECT:
				addName(t.getInternalName());
				break;
		}

	}

	private static void addName(String className) {
		if (className.startsWith("[")) return;
//		Util.log("---> " + className);
		if (!todo.containsKey(className)) {
			todo.put(className, currentLoader);
		}
	}


}
