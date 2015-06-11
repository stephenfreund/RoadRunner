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

package rr.meta;

import java.io.Serializable;

import acme.util.Util;

public class SourceLocation implements Serializable, Comparable<SourceLocation> {

	public static final SourceLocation NULL = new SourceLocation("?", -1, -1);
	
	protected final String file;
	protected final int line;
	protected final int offset;

	public SourceLocation(String file, int line, int offset) {
		this.file = file;
		this.line = line;
		this.offset = offset;
	}

	private SourceLocation(String file, int line) {
		this(file, line, -1);
	}


	@Override
	public String toString() {
		return (this == NULL ? "NullLoc" : file.substring(file.lastIndexOf('/')+1) + ":" + line + (offset > -1 ? ":" + offset : "")).intern();
	}

	public static String toKeyString(String file, int line, int offset) {
		return (file + ":" + line + (offset == -1 ? "" : ":" + offset)).intern();
	}

	public static String toKeyString(String file, int line) {
		return toKeyString(file,line,-1);
	}

	public String getKey() {
		return toKeyString(file, line, offset);
	}

	public String getFile() {
		return file;
	}
	
	public int getLine() {
		return line;
	}
	
	public int getOffset() {
		return offset;
	}

	public int compareTo(SourceLocation loc) {
		int x = file.compareTo(loc.file);
		if (x == 0) x = line - loc.line;
		if (x == 0) x = offset - loc.offset;
		return x;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + line;
		result = prime * result + offset;
	
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		return this.compareTo((SourceLocation)obj) == 0;
	}
	
}
