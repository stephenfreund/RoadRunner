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

package rr.loader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;

import rr.org.objectweb.asm.ClassReader;
import rr.org.objectweb.asm.ClassWriter;
import rr.state.update.AbstractFieldUpdater;
import rr.state.update.CASFieldUpdater;
import rr.state.update.Updaters;
import rr.state.update.Updaters.UpdateMode;
import rr.instrument.Constants;
import rr.instrument.Instrumentor;
import rr.instrument.classes.GuardStateModifierCreator;
import rr.meta.ClassInfo;
import rr.meta.MetaDataInfoMaps;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;
import acme.util.io.URLUtils;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;


public class LoaderContext {

	protected final ClassLoader loader;

	protected static final URLClassLoader bootLoader = new URLClassLoader(URLUtils.getURLArrayFromString(System.getProperty("user.dir"), System.getProperty("sun.boot.class.path")));

	public static final LoaderContext bootLoaderContext = Loader.get(bootLoader);

	public static final CommandLineOption<String> repositoryPathOption = CommandLine.makeString("repository", "", CommandLineOption.Kind.EXPERIMENTAL, "Classpath to search for a class if RoadRunner cannot find it during metadata loading with the standard delegation technique.",
			new Runnable() {
		public void run() {
			repositoryLoader = new URLClassLoader(URLUtils.getURLArrayFromString(System.getProperty("user.dir"), repositoryPathOption.get()));
		}
	});

	protected static URLClassLoader repositoryLoader = null;

	public LoaderContext(ClassLoader loader) {
		this.loader = loader;
	}

	protected URL searchParentChain(String fileName) {
		ClassLoader loader = this.loader;
		if (loader.getParent() != null) {
			URL url = Loader.get(loader.getParent()).searchParentChain(fileName);
			if (url != null) {
				return url;
			}
		}
		return loader.getResource(fileName);

	}

	protected URL searchRepository(String fileName) {
		URL url = null;
		if (repositoryLoader != null) {
			url = repositoryLoader.getResource(fileName);
			if (url == null) {
				Yikes.yikes("Using repository but failed to find: " + fileName);
			} 
		}
		return url;
	}

	protected URL searchAll(String fileName) {
		for (LoaderContext cl: Loader.wrappers.values()) {
			try {
				URL url = cl.getClassLoader().getResource(fileName);
				if (url != null) {
					return url;
				}
			} catch (Throwable e) {
				//				Util.log("Exception while looking for " + fileName + ": " + e);
			}
		}
		return null;
	}


	private Hashtable<String,URL> cache = new Hashtable<String, URL>();

	public ClassInfo getRRClass(final String className) throws ClassNotFoundException {
		try {
			ClassInfo rrClass = MetaDataInfoMaps.getClass(className);
			if (className.startsWith("[")) {
				rrClass.setSuperClass(MetaDataInfoMaps.getClass("java/lang/Object"));
				rrClass.setState(ClassInfo.State.COMPLETE);
				return rrClass;
			}
			if (rrClass.stateAtMost(ClassInfo.State.IN_PRELOAD)) {
				rrClass.setState(ClassInfo.State.IN_PRELOAD);
				try {
					String fileName = className.replace('.', '/') + ".class";

					URL url = null;

					// look in repository
					if (url == null) {
						url = searchRepository(fileName);
					}

					// look in loader caches
					if (url == null) {
						url = cache.get(fileName);
					}

					// look up parent chain
					if (url == null) {
						url = searchParentChain(fileName);
					}

					if (url != null) {
						cache.put(fileName, url);
					}

					// couldn't find it anywhere
					if (url == null) {
						Util.log("Failed To Load Class " + className + "....");
						//						Util.log(StackDump.stackDump());
						throw new ClassNotFoundException(className);
					} else {
						ClassReader reader = new ClassReader(url.openStream());
						MetaDataBuilder.preLoad(LoaderContext.this, reader);
					}
				} catch (IOException e) {
					Assert.fail(e);
				}
			}
			return rrClass;
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (Exception e) {
			Assert.panic(e);
			return null;
		}
	}

	public Class getGuardStateThunkClass(final String className, final String fieldName, final boolean isStatic, final boolean isVolatile)  {
		final String thunkName = Constants.getUpdateThunkName(className, fieldName);
		Class<?> c = loader.findLoadedClass(thunkName);
		if (c != null) return c;
		Class<?> targetClass = loader.findLoadedClass(className.replaceAll("/", "."));
		try {
			Field f = targetClass.getField(fieldName);
		} catch (Exception e) {
			Assert.fail(e);
		}
		byte b[] = Loader.readFromFileCache("updaters", thunkName);
		if (b == null) {
			b = GuardStateModifierCreator.dump(className, fieldName, isStatic, isVolatile);
		}
		return defineClass(thunkName, b);
	}

	public AbstractFieldUpdater getGuardStateThunkObject(final String className, final String fieldName, final boolean isStatic, boolean isVolatile)  {
		try {
			if (Updaters.updateOptions.get() != Updaters.UpdateMode.CAS) {
				Class c = getGuardStateThunkClass(className, fieldName, isStatic, isVolatile);
				return (AbstractFieldUpdater)c.newInstance();
			} else {
				Class<?> targetClass = loader.findLoadedClass(className.replaceAll("/", "."));
				String gsName = Constants.getShadowFieldName(className, fieldName, isStatic, isVolatile);
				Field f = targetClass.getField(gsName);
				if (isStatic) {
					// Fix because CA updaters don't work properly on static fields -- seems to be VM issue
					//   on all platforms...
					synchronized (Updaters.updateOptions) {
						UpdateMode x = Updaters.updateOptions.get();
						Updaters.updateOptions.set(UpdateMode.SAFE);
						Class c = getGuardStateThunkClass(className, fieldName, isStatic, isVolatile);
						Updaters.updateOptions.set(x);
						return (AbstractFieldUpdater)c.newInstance();
					}
				} else {
					return new CASFieldUpdater(f);
				}
			}
		} catch (Exception e) {
			Assert.fail(e);
			return null;
		}
	}

	public synchronized Class<?> defineClass(final String className, byte[] bytes) {

		Class<?> c = loader.findLoadedClass(className);
		if (c == null) {
			c = loader.defineClass(className, bytes, 0, bytes.length);
		}
		return c;
	}

	public ClassLoader getClassLoader() {
		return this.loader;
	}

	public ClassWriter instrument(String className, byte[] b) {
		final ClassReader classReader = new ClassReader(b);
		return Instrumentor.instrument(this, classReader);
	}

	public void sanityCheck(ClassReader classReader) {
		Instrumentor.sanityCheck(this, classReader);	
	}

	@Override
	public String toString() {
		return String.format("%X", System.identityHashCode(this.loader));
	}	
}
