/******************************************************************************

Copyright (c) 2016, Cormac Flanagan (University of California, Santa Cruz)
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

/*
 * Based on code:
 *
 * Copyright (c) 2005, 2009 The Australian National University.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License v2.0.
 * You may obtain the license at
 * 
 *    http://www.opensource.org/licenses/apache2.0.php
 */

import java.io.*;
import java.util.*;

public class RRBench {

	private String[] args;

	public static void main(String args[]) throws Exception {
		RRBench bench = new RRBench(args);
		bench.preIteration();
		bench.iterate();
		bench.postIteration();
		bench.cleanup();
	}

	public RRBench(String[] a) throws Exception {
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

		args = new String[] {
				"@pmd/default.lst",
				"text",
				"scratch/pmd/rulesets/basic.xml",
				"scratch/pmd/rulesets/braces.xml",
				"scratch/pmd/rulesets/codesize.xml",
				"scratch/pmd/rulesets/controversial.xml",
				"scratch/pmd/rulesets/coupling.xml",
				"scratch/pmd/rulesets/design.xml",
				"scratch/pmd/rulesets/favorites.xml",
				"scratch/pmd/rulesets/finalizers.xml",
				"scratch/pmd/rulesets/imports.xml",
				"scratch/pmd/rulesets/javabeans.xml",
				"scratch/pmd/rulesets/junit.xml",
				"scratch/pmd/rulesets/naming.xml",
				"scratch/pmd/rulesets/newrules.xml",
				"scratch/pmd/rulesets/rulesets.properties",
				"scratch/pmd/rulesets/scratchpad.xml",
				"scratch/pmd/rulesets/strictexception.xml",
				"scratch/pmd/rulesets/strings.xml",
				"scratch/pmd/rulesets/unusedcode.xml" 
		};

		args[0] = collectFilesFromFile("scratch/" + args[0].substring(1));
	}

	private String collectFilesFromFile(String inputFileName) {
		try {
			java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(new FileInputStream(inputFileName)));

			List<File> files = new ArrayList<File>();

			for (String l = reader.readLine(); l != null; l = reader.readLine()) {
				files.add(new File("scratch/" + l));
			}
			return commaSeparate(files);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("File " + inputFileName + " error: " + e);
		} catch (java.io.IOException e) {
			throw new RuntimeException("File " + inputFileName + " error: " + e);
		}

	}

	private String commaSeparate(List<File> list) {
		String result = "";
		for (Iterator<File> i = list.iterator(); i.hasNext();) {
			String s = i.next().getPath();
			result += s;
			if (i.hasNext())
				result += ",";
		}
		return result;
	}

	public void preIteration() throws Exception { 
	}

	public void iterate() throws Exception { 
		net.sourceforge.pmd.PMD.main(args);
	}

	public void postIteration() throws Exception {  }

	public void cleanup() throws Exception { 
	}

}
