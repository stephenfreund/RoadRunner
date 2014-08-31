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

package rr.tool;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import rr.state.agent.ThreadStateExtensionAgent;
import acme.util.Assert;
import acme.util.Util;
import acme.util.time.TimedStmt;

public class ToolLoader extends URLClassLoader {

	private static class Entry {
		final String toolName;
		final URL loc;

		public Entry(String toolName, URL loc) {
			super();
			this.toolName = toolName;
			this.loc = loc;
		}
	}

	protected final TreeMap<String, Entry> abbrevs = new TreeMap<String, Entry>();

	public ToolLoader(URL[] urls) {
		super(urls);
		buildToolMap(urls);
	}

	protected void buildToolMap(URL[] urls) {
		final Set<URL> loaded = new HashSet<URL>();
		try {
			Enumeration<URL> e = getResources("rrtools.properties");
			while (e.hasMoreElements()) {
				final URL prop = e.nextElement();
				if (loaded.contains(prop)) {
					continue; // may see same file twice.
				}
				Util.log(new TimedStmt("" + prop) {
					@Override
					public void run() throws Exception {
						Properties p = new Properties();
						p.load(prop.openStream());
						Util.log(p.keySet());

						for (Object s : p.keySet()) {
							if (abbrevs.containsKey(s)) {
								final Entry entry = abbrevs.get(s);
								Util.log("Tool " + s + " is already mapped to "
										+ entry.toolName + " from " + entry.loc);
							} else {
								abbrevs.put((String) s,
										new Entry(p.getProperty((String) s),
												prop));
							}
						}

						loaded.add(prop);
					}
				});
			}
		} catch (Exception e) {
			Assert.panic(e);
		}
	}

	public InputStream getToolAsStream(String x) {
		String expanded = expandAbbrev(x);
		if (expanded == null) {
			expanded = x;
		}
		expanded = expanded.replace(".", "/");
		return getResourceAsStream(expanded + ".class");
	}

	public Class<? extends Tool> loadToolClass(String x) {
		String expanded = expandAbbrev(x);
		if (expanded == null) {
			expanded = x;
		}
		try {
			return (Class<? extends Tool>) this.loadClass(expanded);
		} catch (ClassNotFoundException e) {
			Assert.fail("Cannot find Tool class '" + expanded + "'");
			return null;
		}
	}

	private String expandAbbrev(String x) {
		Entry xa = abbrevs.get(x);
		if (xa != null) {
			return xa.toolName;
		} else {
			return null;
		}
	}

	public void prepToolClass(String x) {
		String expanded = x;
		final String expandAbbrev = expandAbbrev(expanded);
		if (expandAbbrev != null) {
			expanded = expandAbbrev;
		} 
		expanded = expanded.replace(".", "/");
		InputStream in = getToolAsStream(expanded);
		ThreadStateExtensionAgent.registerTool(this, expanded, in);
	}

	@Override
	public String toString() {
		String result = "\n\nKnown Tools:\n\n";
		for (String key : abbrevs.keySet()) {
			Entry v = abbrevs.get(key);
			String tool = v.toolName;
			String pkg = tool.substring(0, tool.lastIndexOf('.'));
			tool = tool.substring(tool.lastIndexOf('.') + 1);
			result += String.format("%6s : %-30s    (package: %s, prop: %s)\n",
					key, tool, pkg, v.loc);
		}
		return result + "\n";
	}

}
