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

import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.commons.Method;

import rr.loader.LoaderContext;
import rr.state.AbstractArrayState;
import rr.tool.RREventGenerator;
import acme.util.Assert;

public class Constants {

	/***** TYPES *****/

	public static final Type SYSTEM_TYPE = Type.getType(java.lang.System.class);
	public static final Type RUNTIME_TYPE = Type.getType(java.lang.Runtime.class);
	public static final Type RR_SYSTEM_TYPE = Type.getType(rr.instrument.java.lang.System.class);
	public static final Type OBJECT_TYPE = Type.getType(java.lang.Object.class);
	public static final Type THREAD_TYPE = Type.getType(java.lang.Thread.class);
	
	public static final Type REFLECT_FIELD_TYPE = Type.getType(java.lang.reflect.Field.class);
	public static final Type REFLECT_ARRAY_TYPE = Type.getType(java.lang.reflect.Array.class);
	public static final Type RR_REFLECT_TYPE = Type.getType(rr.instrument.methods.ReflectionMethodReplacer.class);
	
	
	public static final Type MANAGER_TYPE = Type.getType(rr.tool.RREventGenerator.class);
	public static final Type MANAGER_VALUE_TYPE = Type.getType(rr.tool.RRValueEventGenerator.class);
	public static final Type RR_MAIN_TYPE = Type.getType(rr.RRMain.class);
	public static final Type ACME_UTIL_TYPE = Type.getType(acme.util.Util.class);
	public static final Type THREAD_STATE_TYPE = Type.getObjectType("rr/state/ShadowThread");  // special to avoid loading before tools
	public static final Type GUARD_STATE_TYPE = Type.getObjectType("rr/state/ShadowVar");
	public static final Type SHADOW_VOL_TYPE = Type.getObjectType("rr/state/ShadowVolatile");

	static {
		try {
			LoaderContext.bootLoaderContext.getRRClass(RR_MAIN_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(SYSTEM_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(REFLECT_FIELD_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(REFLECT_ARRAY_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(RR_REFLECT_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(OBJECT_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(MANAGER_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(THREAD_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(SHADOW_VOL_TYPE.getInternalName());
			LoaderContext.bootLoaderContext.getRRClass(ACME_UTIL_TYPE.getInternalName());
		} catch (ClassNotFoundException e) {
			Assert.panic(e);
		}
	}

	
	/***** METHODS *****/
	
	public static final Method CURRENT_THREAD_METHOD =	new Method("getCurrentShadowThread", THREAD_STATE_TYPE, new Type[] { } );
	
	private static final Method READ_ACCESS_METHOD = new Method("readAccess", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	private static final Method WRITE_ACCESS_METHOD = new Method("writeAccess", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	private static final Method VOLATILE_READ_ACCESS_METHOD = new Method("volatileReadAccess", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	private static final Method VOLATILE_WRITE_ACCESS_METHOD = new Method("volatileWriteAccess", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });

	public static final Method SHADOW_VOL_GET_METHOD = new Method("get", SHADOW_VOL_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE });

	// for multiple classloaders...
	private static final Method READ_ACCESS_METHOD_ML = new Method("readAccessML", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	private static final Method WRITE_ACCESS_METHOD_ML = new Method("writeAccessML", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	private static final Method VOLATILE_READ_ACCESS_METHOD_ML = new Method("volatileReadAccessML", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	private static final Method VOLATILE_WRITE_ACCESS_METHOD_ML = new Method("volatileWriteAccessML", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });

	public static final Method READ_ARRAY_METHOD = new Method("arrayRead", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	public static final Method READ_ARRAY_WITH_UPDATER_METHOD = new Method("arrayRead", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, Type.getType(AbstractArrayState.class) });
	public static final Method WRITE_ARRAY_METHOD = new Method("arrayWrite", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	public static final Method WRITE_ARRAY_WITH_UPDATER_METHOD = new Method("arrayWrite", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, Type.getType(AbstractArrayState.class) });
	public static final Method ACQUIRE_METHOD = new Method("acquire", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	public static final Method TEST_ACQUIRE_METHOD = new Method("testAcquire", Type.BOOLEAN_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	public static final Method RELEASE_METHOD = new Method("release", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	public static final Method TEST_RELEASE_METHOD = new Method("testRelease", Type.BOOLEAN_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	public static final Method ENTER_METHOD = new Method("enter", Type.VOID_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE });
	public static final Method EXIT_METHOD = new Method("exit", Type.VOID_TYPE, new Type[] { THREAD_STATE_TYPE });
	public static final Method INVOKE_METHOD = new Method("invoke", Type.VOID_TYPE, new Type[] { Type.INT_TYPE, THREAD_STATE_TYPE });

	public static final Method READ_ACCESS_METHOD_WITH_VALUES = new Method("readAccess", OBJECT_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, OBJECT_TYPE });
	public static final Method WRITE_ACCESS_METHOD_WITH_VALUES = new Method("writeAccess", OBJECT_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, OBJECT_TYPE, OBJECT_TYPE});
	public static final Method READ_ARRAY_METHOD_WITH_VALUES = new Method("arrayRead", OBJECT_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, OBJECT_TYPE });	
	public static final Method WRITE_ARRAY_METHOD_WITH_VALUES = new Method("arrayWrite", OBJECT_TYPE, new Type[] { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, OBJECT_TYPE, OBJECT_TYPE });
	public static final Method VOLATILE_READ_ACCESS_METHOD_WITH_VALUES = new Method("volatileReadAccess", OBJECT_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, OBJECT_TYPE });
	public static final Method VOLATILE_WRITE_ACCESS_METHOD_WITH_VALUES = new Method("volatileWriteAccess", OBJECT_TYPE, new Type[] { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, OBJECT_TYPE, OBJECT_TYPE});

	public static final Method READ_FP_METHOD = Method.getMethod("boolean readFastPath(rr.state.ShadowVar, rr.state.ShadowThread)");
	public static final Method WRITE_FP_METHOD =  Method.getMethod("boolean writeFastPath(rr.state.ShadowVar, rr.state.ShadowThread)");
	public static final Method ARRAY_READ_FP_METHOD = Method.getMethod("boolean arrayReadFastPath(int, rr.state.ShadowVar, rr.state.ShadowThread)");
	public static final Method ARRAY_WRITE_FP_METHOD =  Method.getMethod("boolean arrayWriteFastPath(int, rr.state.ShadowVar, rr.state.ShadowThread)");
	public static final Method FIELD_READ_FP_METHOD = Method.getMethod("boolean fieldReadFastPath(int, rr.state.ShadowVar, rr.state.ShadowThread)");
	public static final Method FIELD_WRITE_FP_METHOD =  Method.getMethod("boolean fieldWriteFastPath(int, rr.state.ShadowVar, rr.state.ShadowThread)");


	
	/***** NAMING *****/
	
	protected static final String PREFIX = "__$rr_";
	protected static final String SUFFIX = "__$rr_";
	protected static final String GET_PREFIX = PREFIX + "get_";
	protected static final String PUT_PREFIX = PREFIX + "put_";
	protected static final String VAR_STATE_SUFFIX = SUFFIX + "_Shadow_";
	protected static final String THREAD_STATE_SUFFIX = SUFFIX + "_with_ThreadState_";
	protected static final String THUNK_SUFFIX = SUFFIX + "_Update_";
	protected static final String ORIGINAL_CODE_SUFFIX = SUFFIX + "_Original_";
	protected static final String SYNCHRONIZED_THUNK_SUFFIX = SUFFIX + "_Sync_";

	
	public static boolean isSyntheticName(String name) {
		return name.contains(PREFIX);
	}
	

	public static String getShadowFieldName(String owner, String name, boolean isStatic, boolean isVolatile) {
		if (Instrumentor.fieldOption.get() == Instrumentor.FieldMode.FINE || isStatic || isVolatile) {
			return PREFIX + name;
		} else {
			String s = PREFIX + (Instrumentor.fieldOption.get() == Instrumentor.FieldMode.COARSE ? "" : owner.replace("/", "_")) + VAR_STATE_SUFFIX + (isStatic ? "static" : "instance");
			return s;
		}
	}

	public static String getThreadLocalName(String name) {
		return PREFIX + name + THREAD_STATE_SUFFIX; 
	}

	public static String getSyncName(String name) {
		return PREFIX + name + SYNCHRONIZED_THUNK_SUFFIX; 
	}

	public static String getOrigName(String name) {
		return PREFIX + name + ORIGINAL_CODE_SUFFIX; 
	}

	public static String getUpdateThunkName(String className, String fieldName) {
		className = mungeClassName(className);
		return PREFIX + className + THUNK_SUFFIX + fieldName;
	}


	private static String mungeClassName(String className) {
		className = className.replace('/', '_');
		return className;
	}


	public static String recoverOriginalNameFromMangled(String name) {
		if (name.contains(PREFIX)) {
			while(name.startsWith(PREFIX)) {
				name = name.substring(PREFIX.length());
			}
			final int x = name.indexOf(SUFFIX);
			if (x != -1) {
				name = name.substring(0, x);
			}
		}
		return name;
	}


	public static Method getAccessMethod(String className, String name, String desc, boolean isPut) {
		className = mungeClassName(className);
		if (isPut) {
			return new Method(PUT_PREFIX + name, "(" + desc + "I" + THREAD_STATE_TYPE.getDescriptor() + ")V");
		} else {
			return new Method(GET_PREFIX + name, "(" + "I" + THREAD_STATE_TYPE.getDescriptor() + ")" + desc);
		}
	}
	
	public static Method getWriteAccessWithValueMethod(Type t) {
		final int sort = t.getSort();
		Type tt = t;
		if (sort == Type.OBJECT || sort == Type.ARRAY) {
			tt = Constants.OBJECT_TYPE;
		}
		Type args[] = { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, tt, tt };
		Method m = new Method("writeAccess", tt, args);
		return m;
	}

	public static Method getReadAccessWithValueMethod(Type t) {
		final int sort = t.getSort();
		Type tt = t;
		if (sort == Type.OBJECT || sort == Type.ARRAY) {
			tt = Constants.OBJECT_TYPE;
		}
		Type args[] = { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, tt };
		Method m = new Method("readAccess", tt, args);
		return m;
	}

	
	public static Method getVolatileWriteAccessWithValueMethod(Type t) {
		final int sort = t.getSort();
		Type tt = t;
		if (sort == Type.OBJECT || sort == Type.ARRAY) {
			tt = Constants.OBJECT_TYPE;
		}
		Type args[] = { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, tt, tt };
		Method m = new Method("volatileWriteAccess", tt, args);
		return m;
	}

	public static Method getVolatileReadAccessWithValueMethod(Type t) {
		final int sort = t.getSort();
		Type tt = t;
		if (sort == Type.OBJECT || sort == Type.ARRAY) {
			tt = Constants.OBJECT_TYPE;
		}
		Type args[] = { OBJECT_TYPE, GUARD_STATE_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, tt };
		Method m = new Method("volatileReadAccess", tt, args);
		return m;
	}

	
	public static Method getWriteArrayAccessWithValueMethod(Type t) {
		final int sort = t.getSort();
		Type tt = t;
		if (sort == Type.OBJECT || sort == Type.ARRAY) {
			tt = Constants.OBJECT_TYPE;
		}
		Type args[] = { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, tt, tt };
		Method m = new Method("arrayWrite", tt, args);
		return m;
	}

	public static Method getReadArrayAccessWithValueMethod(Type t) {
		final int sort = t.getSort();
		Type tt = t;
		if (sort == Type.OBJECT || sort == Type.ARRAY) {
			tt = Constants.OBJECT_TYPE;
		}
		Type args[] = { OBJECT_TYPE, Type.INT_TYPE, Type.INT_TYPE, THREAD_STATE_TYPE, tt };
		Method m = new Method("arrayRead", tt, args);
		return m;
	}


	public static Method getREAD_ACCESS_METHOD() {
		return RREventGenerator.multiClassLoaderOption.get() ? READ_ACCESS_METHOD_ML : READ_ACCESS_METHOD;
	}


	public static Method getWRITE_ACCESS_METHOD() {
		return RREventGenerator.multiClassLoaderOption.get() ? WRITE_ACCESS_METHOD_ML : WRITE_ACCESS_METHOD;
	}


	public static Method getVOLATILE_READ_ACCESS_METHOD() {
		return RREventGenerator.multiClassLoaderOption.get() ? VOLATILE_READ_ACCESS_METHOD_ML : VOLATILE_READ_ACCESS_METHOD;
	}


	public static Method getVOLATILE_WRITE_ACCESS_METHOD() {
		return RREventGenerator.multiClassLoaderOption.get() ? VOLATILE_WRITE_ACCESS_METHOD_ML : VOLATILE_WRITE_ACCESS_METHOD;
	}


}
