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

package rr.instrument.methods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import rr.instrument.Constants;
import rr.loader.LoaderContext;
import rr.meta.ClassInfo;
import rr.meta.FieldAccessInfo;
import rr.meta.FieldInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import rr.meta.SourceLocation;
import rr.org.objectweb.asm.Label;
import rr.org.objectweb.asm.MethodVisitor;
import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.Method;
import rr.state.ShadowThread;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.count.Counter;

public class ReflectionMethodReplacer extends RRMethodAdapter {

	public ReflectionMethodReplacer(MethodVisitor mv, MethodInfo m) {
		super(mv, m);
		try {
			LoaderContext.bootLoaderContext.getRRClass("rr/instrument/methods/ReflectionMethodReplacer");
			LoaderContext.bootLoaderContext.getRRClass("java/lang/reflect/Field");
			LoaderContext.bootLoaderContext.getRRClass("java/lang/reflect/Array");
		} catch (ClassNotFoundException e) {
			Assert.panic(e);
		}
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean isInterface) { 
		if (owner.startsWith("java/lang/reflect/Field") ||
				owner.startsWith("java/lang/reflect/Array")) {

			for (Replacement r : replacements) {
				if (r.matches(opcode, owner, name, desc)) {
					reflectionCounter.inc();
					Util.logf("Replace %s.%s%s", owner, name, desc);
					r.replace(opcode, this);
					return;
				}
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc, isInterface);
	}

	public static Counter reflectionCounter = new Counter("Reflection", "Source Locs");
	public static Counter reflectionRuntimeCounter = new Counter("Reflection", "Run-time Calls");


	protected static Vector<Integer> onlyVirtualOpcode = new Vector<Integer>();
	static {
		onlyVirtualOpcode.add(Opcodes.INVOKEVIRTUAL);
	}

	protected static Vector<Integer> onlyStaticOpcode = new Vector<Integer>();
	static {
		onlyStaticOpcode.add(Opcodes.INVOKESTATIC);
	}

	private static class FieldGet extends Replacement {
		private Type t;
		private String suffix;
		public FieldGet(Type t, String suffix) {
			super(onlyVirtualOpcode, Constants.REFLECT_FIELD_TYPE, Method.getMethod(t.getClassName() + " get" + suffix + "(java.lang.Object)"));
			this.t = t;
			this.suffix=suffix;
		}

		@Override
		public void replace(int opcode, RRMethodAdapter gen) {
			gen.invokeStatic(Constants.RR_REFLECT_TYPE,new Method("get" + suffix,t, new Type[] {  Constants.REFLECT_FIELD_TYPE, Constants.OBJECT_TYPE }));			
		}

	}

	private static class FieldSet extends Replacement {
		private Type t;
		private String suffix;
		public FieldSet(Type t, String suffix) {
			super(onlyVirtualOpcode, Constants.REFLECT_FIELD_TYPE, Method.getMethod("void set" + suffix + "(java.lang.Object, " + t.getClassName() + ")"));
			this.t = t;
			this.suffix=suffix;
		}

		@Override
		public void replace(int opcode, RRMethodAdapter gen) {
			gen.invokeStatic(Constants.RR_REFLECT_TYPE,new Method("set" + suffix,Type.VOID_TYPE, new Type[] {  Constants.REFLECT_FIELD_TYPE, Constants.OBJECT_TYPE, t }));			
		}

	}

	private static class ArrayGet extends Replacement {
		private Type t;
		public ArrayGet(Type t, String suffix) {
			super(onlyStaticOpcode, Constants.REFLECT_ARRAY_TYPE, Method.getMethod(t.getClassName() + " get" + suffix + "(java.lang.Object, int)"));
			this.t = t;
		}

		@Override
		public void replace(int opcode, RRMethodAdapter gen) {
			gen.swap();
			gen.checkCast(Type.getType("[" + t.getDescriptor()));
			gen.swap();
			gen.arrayLoad(t);
		}

	}

	private static Class<?> primitiveClasses[] = { 
		Integer.TYPE, Boolean.TYPE, Byte.TYPE, Character.TYPE, Short.TYPE, Long.TYPE, Double.TYPE, Float.TYPE
	};


	
	private static class ArrayGetObject extends Replacement {
		public ArrayGetObject() {
			super(onlyStaticOpcode, Constants.REFLECT_ARRAY_TYPE, Method.getMethod("java.lang.Object get(java.lang.Object, int)"));
		}

		@Override
		public void replace(int opcode, RRMethodAdapter gen) {
			Label end = new Label();
			for (Class c : primitiveClasses) {
				Label next = new Label();
				Type t = Type.getType(c);
				gen.dup2();
				gen.pop();
				gen.instanceOf(Type.getType("[" + t.getDescriptor()));
				gen.ifZCmp(EQ, next);
				gen.swap();
				gen.checkCast(Type.getType("[" + t.getDescriptor()));
				gen.swap();
				gen.arrayLoad(t);
				gen.box(t);
				gen.goTo(end);
				gen.visitLabel(next);
			}
			gen.swap();
			gen.checkCast(Type.getType("[" + Constants.OBJECT_TYPE.getDescriptor()));
			gen.swap();
			gen.arrayLoad(Constants.OBJECT_TYPE);
			gen.visitLabel(end);
		}
	}

	private static class ArraySet extends Replacement {
		private Type t;
		public ArraySet(Type t, String suffix) {
			super(onlyStaticOpcode, Constants.REFLECT_ARRAY_TYPE, Method.getMethod("void set" + suffix + "(java.lang.Object, int, " + t.getClassName() + ")"));
			this.t = t;
		}

		@Override
		public void replace(int opcode, RRMethodAdapter gen) {
			gen.dup2X1();
			gen.pop2();
			gen.checkCast(Type.getType("[" + t.getDescriptor()));
			gen.dupX2();
			gen.pop();
			gen.arrayStore(t);
		}
	}

	private static class ArraySetBig extends Replacement {
		private Type t;
		public ArraySetBig(Type t, String suffix) {
			super(onlyStaticOpcode, Constants.REFLECT_ARRAY_TYPE, Method.getMethod("void set" + suffix + "(java.lang.Object, int, " + t.getClassName() + ")"));
			this.t = t;
		}

		@Override
		public void replace(int opcode, RRMethodAdapter gen) {
			gen.dup2X2();
			gen.pop2();
			gen.swap();
			gen.checkCast(Type.getType("[" + t.getDescriptor()));
			gen.swap();
			gen.dup2X2();
			gen.pop2();
			gen.arrayStore(t);
		}
	}

	private static class ArraySetObject extends Replacement {
		public ArraySetObject() {
			super(onlyStaticOpcode, Constants.REFLECT_ARRAY_TYPE, Method.getMethod("java.lang.Object get(java.lang.Object, int, java.lang.Object)"));
		}

		@Override
		public void replace(int opcode, RRMethodAdapter gen) {
			Label end = new Label();
			for (Class c : primitiveClasses) {
				Label next = new Label();
				Type t = Type.getType(c);
				gen.dup();
				gen.instanceOf(Type.getType(t.getDescriptor()));
				gen.ifZCmp(EQ, next);	
				gen.unbox(Type.getType(t.getDescriptor()));
				if (t != Type.LONG_TYPE && t != Type.DOUBLE_TYPE) {
					gen.dup2X1();
					gen.pop2();
					gen.checkCast(Type.getType("[" + t.getDescriptor()));
					gen.dupX2();
					gen.pop();
				} else {
					gen.dup2X2();
					gen.pop2();
					gen.swap();
					gen.checkCast(Type.getType("[" + t.getDescriptor()));
					gen.swap();
					gen.dup2X2();
					gen.pop2();
				}
				gen.arrayStore(t);				
				gen.goTo(end);
				gen.visitLabel(next);
			}
			gen.dup2X1();
			gen.pop2();
			gen.checkCast(Type.getType("[" + Constants.OBJECT_TYPE.getDescriptor()));
			gen.dupX2();
			gen.pop();
			gen.arrayStore(Constants.OBJECT_TYPE);
			gen.visitLabel(end);
		}

	}



	protected static Replacement[] replacements = new Replacement[] {
		new FieldGet(Constants.OBJECT_TYPE, ""),
		new FieldGet(Type.INT_TYPE, "Int"),
		new FieldGet(Type.BOOLEAN_TYPE, "Boolean"),
		new FieldGet(Type.BYTE_TYPE, "Byte"),
		new FieldGet(Type.CHAR_TYPE, "Char"),
		new FieldGet(Type.DOUBLE_TYPE, "Double"),
		new FieldGet(Type.FLOAT_TYPE, "Float"),
		new FieldGet(Type.LONG_TYPE, "Long"),
		new FieldGet(Type.SHORT_TYPE, "Short"),
		new FieldSet(Constants.OBJECT_TYPE, ""),
		new FieldSet(Type.INT_TYPE, "Int"),
		new FieldSet(Type.BOOLEAN_TYPE, "Boolean"),
		new FieldSet(Type.BYTE_TYPE, "Byte"),
		new FieldSet(Type.CHAR_TYPE, "Char"),
		new FieldSet(Type.DOUBLE_TYPE, "Double"),
		new FieldSet(Type.FLOAT_TYPE, "Float"),
		new FieldSet(Type.LONG_TYPE, "Long"),
		new FieldSet(Type.SHORT_TYPE, "Short"),
		new ArrayGetObject(),
		new ArrayGet(Type.INT_TYPE, "Int"),
		new ArrayGet(Type.BOOLEAN_TYPE, "Boolean"),
		new ArrayGet(Type.BYTE_TYPE, "Byte"),
		new ArrayGet(Type.CHAR_TYPE, "Char"),
		new ArrayGet(Type.DOUBLE_TYPE, "Double"),
		new ArrayGet(Type.FLOAT_TYPE, "Float"),
		new ArrayGet(Type.LONG_TYPE, "Long"),
		new ArrayGet(Type.SHORT_TYPE, "Short"),
		new ArraySetObject(),
		new ArraySet(Type.INT_TYPE, "Int"),
		new ArraySet(Type.BOOLEAN_TYPE, "Boolean"),
		new ArraySet(Type.BYTE_TYPE, "Byte"),
		new ArraySet(Type.CHAR_TYPE, "Char"),
		new ArraySetBig(Type.DOUBLE_TYPE, "Double"),
		new ArraySet(Type.FLOAT_TYPE, "Float"),
		new ArraySetBig(Type.LONG_TYPE, "Long"),
		new ArraySet(Type.SHORT_TYPE, "Short"),
	};

	//**//


	private static class ReflectInfo {
		final FieldAccessInfo fai;
		final java.lang.reflect.Method method;
		public ReflectInfo(FieldAccessInfo fai, java.lang.reflect.Method method) {
			this.fai = fai;
			this.method = method;
		}

	}


	private static HashMap<java.lang.reflect.Field, ReflectInfo> reads = new HashMap<java.lang.reflect.Field, ReflectInfo>();
	private static HashMap<java.lang.reflect.Field, ReflectInfo> writes = new HashMap<java.lang.reflect.Field, ReflectInfo>();

	private static synchronized ReflectInfo getReadInfo(java.lang.reflect.Field fd, FieldInfo field) {
		if (reads.get(fd) == null) {
			Class owner = fd.getDeclaringClass();
			String name = fd.getName();
			String desc = fd.getType().isPrimitive() ? Type.getDescriptor(fd.getType()) : "Ljava/lang/Object;";
			Method m = Constants.getAccessMethod(field.getOwner().getName(), name, desc, false);
			try {
				java.lang.reflect.Method meth = owner.getDeclaredMethod(m.getName(), int.class, ShadowThread.class);
				MethodInfo info = MetaDataInfoMaps.getMethod(MetaDataInfoMaps.getClass("java/lang/reflect/Field"), "reflectionGet" + field.getId(), "()"+desc);
				info.setFlags((fd.getModifiers() & Modifier.STATIC) != 0, true, false);
				FieldAccessInfo fai = MetaDataInfoMaps.makeFieldAccess(SourceLocation.NULL, info, false, field);
				reads.put(fd, new ReflectInfo(fai, meth));
			} catch (Exception e) {
				Assert.panic(e);
			}
		} 
		return reads.get(fd);
	}	



	private static synchronized ReflectInfo getWriteInfo(java.lang.reflect.Field fd, FieldInfo field) {
		if (writes.get(fd) == null) {
			Class owner = fd.getDeclaringClass();
			String name = fd.getName();
			String desc = fd.getType().isPrimitive() ? Type.getDescriptor(fd.getType()) : "Ljava/lang/Object;";
			Method m = Constants.getAccessMethod(field.getOwner().getName(), name, desc, true);
			try {
				java.lang.reflect.Method meth = owner.getDeclaredMethod(m.getName(), fd.getType(), int.class, ShadowThread.class);
				MethodInfo info = MetaDataInfoMaps.getMethod(MetaDataInfoMaps.getClass("java/lang/reflect/Field"), "reflectionPut" + field.getId(), "(" + desc + ")V");
				info.setFlags((fd.getModifiers() & Modifier.STATIC) != 0, true, false);
				FieldAccessInfo fai = MetaDataInfoMaps.makeFieldAccess(SourceLocation.NULL, info, true, field);
				writes.put(fd, new ReflectInfo(fai, meth));
			} catch (Exception e) {
				Assert.panic(e);
			}
		} 
		return writes.get(fd);
	}	


	public static java.lang.Object get(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		Class owner = fd.getDeclaringClass();
		String name = fd.getName();
		ClassInfo ownerClass = MetaDataInfoMaps.getClass(owner.getName().replace('.', '/'));
		FieldInfo field = MetaDataInfoMaps.getField(ownerClass, name, Type.getDescriptor(fd.getType()));
		if (!InstrumentationFilter.shouldInstrument(field)) {
			return fd.get(o);
		} else {
			ReflectInfo info = getReadInfo(fd, field);
			if (info != null) {
				try {
					ReflectionMethodReplacer.reflectionRuntimeCounter.inc();
					return info.method.invoke(o, info.fai.getId(), ShadowThread.getCurrentShadowThread());
				} catch (InvocationTargetException e) {
					Assert.panic(e);
					return null;
				}
			}
			return null;
		}
	}

	public static boolean getBoolean(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Boolean)get(fd, o);
	}
	public static byte getByte(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Byte)get(fd, o);
	}
	public static char getChar(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Character)get(fd, o);
	}
	public static short getShort(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Short)get(fd, o);
	}
	public static int getInt(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Integer)get(fd, o);
	}
	public static long getLong(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Long)get(fd, o);
	}
	public static float getFloat(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Float)get(fd, o);
	}
	public static double getDouble(java.lang.reflect.Field fd, java.lang.Object o) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException {
		return (Double)get(fd, o);
	}

	public static void set(java.lang.reflect.Field fd, java.lang.Object o, java.lang.Object v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		Class owner = fd.getDeclaringClass();
		String name = fd.getName();
		ClassInfo ownerClass = MetaDataInfoMaps.getClass(owner.getName().replace('.', '/'));
		FieldInfo field = MetaDataInfoMaps.getField(ownerClass, name, Type.getDescriptor(fd.getType()));
		if (!InstrumentationFilter.shouldInstrument(field)) {
			if (fd.getType() == rr.state.ShadowVar.class) {
				Yikes.yikes("Reflective Write to ShadowVar --- ignoring! (" + fd + ")");
			} else {
				fd.set(o, v);
			}
		} else {
			ReflectInfo info = getWriteInfo(fd, field);
			if (info != null) {
				try {
					ReflectionMethodReplacer.reflectionRuntimeCounter.inc(); 
					info.method.invoke(o, v, info.fai.getId(), ShadowThread.getCurrentShadowThread());
				} catch (InvocationTargetException e) {
					Assert.panic(e);
				}
			}
		}
	}
	public static void setBoolean(java.lang.reflect.Field fd, java.lang.Object o, boolean v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
	public static void setByte(java.lang.reflect.Field fd, java.lang.Object o, byte v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
	public static void setChar(java.lang.reflect.Field fd, java.lang.Object o, char v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
	public static void setShort(java.lang.reflect.Field fd, java.lang.Object o, short v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
	public static void setInt(java.lang.reflect.Field fd, java.lang.Object o, int v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
	public static void setLong(java.lang.reflect.Field fd, java.lang.Object o, long v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
	public static void setFloat(java.lang.reflect.Field fd, java.lang.Object o, float v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
	public static void setDouble(java.lang.reflect.Field fd, java.lang.Object o, double v) throws java.lang.IllegalArgumentException, java.lang.IllegalAccessException { 
		set(fd, o, v);
	}
}
