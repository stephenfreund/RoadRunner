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

package acme.util.io;

import java.io.PrintWriter;
import java.util.Stack;

/**
 * A writer to facilitate generating proper XML.
 */
public class XMLWriter {

	protected PrintWriter out;
	protected Stack<String> openTags = new Stack<String>();
	
	public XMLWriter(PrintWriter out) {
		this.out = out;
	}
	
	public void close() {
		out.close();
	}
	
	protected void pad() {
		for (int i = 0; i < openTags.size(); i++) {
			out.print("  ");
		}
	}
	
	/**
	 * Enter the scope of a new XML tag.
	 */
	public void push(String tag) {
		pad();
		out.println("<" + tag + ">");
		openTags.push(tag);
	}
	
	/**
	 * Leave the scope of a tag
	 */
	public void pop() {
		String tag = openTags.pop();
		pad();
		out.println("</" + tag + ">");
	}

	/**
	 * Takes a flag to include a newline and pairs stored in an array:
	 * 
	 * printWithFixedWidths(true, "k1", v1, "k2", v2);
	 */
	protected void print(boolean addNewLine, Object... keysAndValues) {
		for (int i = 0; i < keysAndValues.length; i += 2) {
			String p = String.format("<%s> %s </%s> ", keysAndValues[i], keysAndValues[i+1], keysAndValues[i]);
			out.print(p);
		}
		if (addNewLine) out.println();
	}

	/**
	 * Takes a tag and the triples of key-value-width:
	 * 
	 * printWithFixedWidths("k1", v1, 10, "k2", v2, -12);
	 */
	public void printWithFixedWidths(boolean addNewLine, Object... keysValuesAndWidths) {
		for (int i = 0; i < keysValuesAndWidths.length; i += 3) {
			String p = String.format("<%s> %s </%s> ", keysValuesAndWidths[i], keysValuesAndWidths[i+1], keysValuesAndWidths[i]);
			out.printf("%" + keysValuesAndWidths[i+2] + "s", p);
		}
		if (addNewLine) out.println();
	}
	
	
	public void print(Object... keysAndValues) {
		pad();
		print(true, keysAndValues);
	}

	/**
	 * Takes a tag and the triples of key-value-width:
	 * 
	 * printWithFixedWidths("k1", v1, 10, "k2", v2, -12);
	 */
	public void printWithFixedWidths(Object... keysValuesAndWidths) {
		pad();
		printWithFixedWidths(true, keysValuesAndWidths);
	}
	
	/**
	 * Takes a tag and then key-value pairs
	 */
	public void printInsideScope(String tag, Object... keysAndValues) {
		pad();
		out.printf("<%s> ", tag);
		this.print(false, keysAndValues);
		out.printf("</%s>", tag);
		out.println();
	}

	/**
	 * Takes a tag and the triples of key-value-width:
	 * 
	 * printInsideScopeWithFixedWidths("tag", "k1", v1, 10, "k2", v2, -12);
	 */
	public void printInsideScopeWithFixedWidths(String tag, Object... keysValuesAndWidths) {
		pad();
		out.printf("<%s>", tag);
		this.printWithFixedWidths(false, keysValuesAndWidths);
		out.printf("</%s>", tag);
		out.println();
	}
	
	public void blank() {
		out.println();
	}
}
