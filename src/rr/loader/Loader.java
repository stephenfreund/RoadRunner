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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import rr.org.objectweb.asm.commons.Method;

import rr.instrument.Instrumentor;
import rr.instrument.hooks.SpecialMethodListener;
import rr.instrument.hooks.SpecialMethods;
import rr.meta.ClassInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoKeys;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MetaDataInfoVisitor;
import acme.util.Assert;
import acme.util.io.XMLWriter;

public class Loader {

	static final ConcurrentHashMap<ClassLoader, LoaderContext> wrappers = new ConcurrentHashMap<ClassLoader, LoaderContext>();
	public static final Hashtable<String, LoaderContext> classes = new Hashtable<String, LoaderContext>();


	public static synchronized LoaderContext get(ClassLoader loader) {
		if (loader == null) return null;
		LoaderContext w = wrappers.get(loader);
		if (w == null) {
			w = new LoaderContext(loader);
			wrappers.put(loader, w);
		}
		return w;
	}


	private static final Vector<MetaDataInfoVisitor> visitors = new Vector<MetaDataInfoVisitor>();

	public synchronized static void addListener(MetaDataInfoVisitor visitor) {
		visitors.add(visitor);
	}

	public synchronized static void notify(ClassInfo rrClass) {
		for (MetaDataInfoVisitor v : visitors) {
			rrClass.accept(v);
		}
	}

	public synchronized static void addToolSpecificReplacement(String cName, String methodString, SpecialMethodListener listener) {
		Method method = Method.getMethod(methodString);
		String m = MetaDataInfoKeys.getMethodKey(cName, method.getName(), method.getDescriptor()).replaceAll("\\(", "\\\\(").replaceAll("\\)","\\\\)");
		InstrumentationFilter.methodsToWatch.get().addFirst("-" + m);

		SpecialMethods.addHook(cName, methodString, listener);
	}

	public static synchronized LoaderContext loaderForClass(String name) {
		LoaderContext w = classes.get(name);
		return w;
	}

	public static final Vector<String> instrumentedFiles = new Vector<String>();
	protected static final Vector<String> skippedFiles = new Vector<String>();
	protected static final Vector<String> sanityCheckedFiles = new Vector<String>();


	public static void printXML(XMLWriter xml) {
		String r = "";

		Collections.sort(instrumentedFiles);
		Collections.sort(skippedFiles);
		Collections.sort(sanityCheckedFiles);

		for (String s : instrumentedFiles) {
			r += s + " ";
		}
		xml.print("instrumented", r);
		xml.print("instrumentedNum", instrumentedFiles.size() + "");

		r = "";
		for (String s : skippedFiles) {
			r += s + " ";
		}
		xml.print("skipped", r + "");
		xml.print("skippedNum", skippedFiles.size() + "");

		r = "";
		for (String s : sanityCheckedFiles) {
			r += s + " ";
		}
		xml.print("sanityChecked", r + "");
		xml.print("sanityCheckedNum", sanityCheckedFiles.size() + "");
	}


	public static final void writeToFileCache(String prefix, String className, byte b[]) {
		final String dump = Instrumentor.dumpClassOption.get();
		if (!dump.equals("")) {
			String path = dump + "/" + prefix + "/" + className;
			String dirPath = path.substring(0,path.lastIndexOf("/"));
			new File(dirPath).mkdirs();
			FileOutputStream fos;
			try {
				String name = path + ".class";
				fos = new FileOutputStream(name);
				fos.write(b);
				fos.close();
			} catch (Exception e) {
				Assert.fail(e);
			} 
		}
	}


	public static final byte[] readFromFileCache(String prefix, String className) {
		final String cached = MetaDataInfoMaps.metaOption.get();
		if (cached != null) {
			try {
				String name = cached + "/" +  prefix + "/" + className + ".class";
				FileInputStream fis;
				fis = new FileInputStream(name);
				byte[] b = new byte[fis.available()];
				fis.read(b);
				fis.close();
				return b;
			} catch (Exception e) {
				return null;
			} 
		} else {
			return null;
		}
	}
}
