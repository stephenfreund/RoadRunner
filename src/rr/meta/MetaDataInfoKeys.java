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

public class MetaDataInfoKeys {

	private static int counter = 0;
	
	public static String getClassKey(String className) {
		return className; 
	}

	public static String getInterfaceKey(String className) {
		return className; 
	}

	public static String getFieldKey(ClassInfo rrClass, String fieldName, String descriptor) {
		return getFieldKey(rrClass.getName(), fieldName, descriptor);
	}

	public static String getFieldKey(String rrClass, String fieldName, String descriptor) {
		return (rrClass + "." + fieldName + "_" + descriptor.replace("[", "\\[")).intern(); 
	}

	public static String getMethodKey(ClassInfo rrClass, String methodName, String signature) {
		return getMethodKey(rrClass.getName(), methodName, signature); 
	}

	public static String getMethodKey(String rrClass, String methodName, String signature) {
		return (rrClass + "." + methodName + signature).intern(); 
	}

	private static String getLockKey(String fileName, int line, int offset, boolean isAcquire) {
		return ((isAcquire ? "acq":"rel") + "_lock@" + SourceLocation.toKeyString(fileName, line) + ":" + offset).intern(); 
	}
	
	public static String getArrayAccessKey(String fileName, int line, int offset, boolean isWrite) {
		return ((isWrite ? "wr":"rd") + "_array@" + SourceLocation.toKeyString(fileName, line) + ":" + offset).intern(); 
	}

	public static String getFieldAccessKey(String fileName, int line, int offset, FieldInfo field, boolean isWrite) {
		return ((isWrite ? "wr":"rd") + "_" + (field == null ? "null" : field.getKey()) + "@" + SourceLocation.toKeyString(fileName, line) + ":" + offset).intern(); 
	}

	public static String getJoinKey(String fileName, int line) {
		return "join@" + SourceLocation.toKeyString(fileName, line); 
	}

	public static String getStartKey(String fileName, int line) {
		return ("start@" + SourceLocation.toKeyString(fileName, line)).intern(); 
	}

	public static String getWaitKey(String fileName, int line) {
		return ("wait@" + SourceLocation.toKeyString(fileName, line)).intern(); 
	}

	public static String getInterruptKey(String fileName, int line) {
		return ("interrupt@" + SourceLocation.toKeyString(fileName, line)).intern(); 
	}
	

	public static String getInvokeKey(String fileName, int line, int offset, MethodInfo m) {
		return ("call_" + (m == null ? "null" : m.getKey()) + "@" + SourceLocation.toKeyString(fileName, line, offset)).intern(); 
	}
	
	public static String getLockKey(SourceLocation loc, boolean isAcquire) {
		return getLockKey(loc.getFile(), loc.getLine(), loc.getOffset(), isAcquire);
	}
	
	public static String getArrayAccessKey(SourceLocation loc, boolean isWrite) {
		return getArrayAccessKey(loc.getFile(), loc.getLine(), loc.getOffset(), isWrite);
	}

	public static String getFieldAccessKey(SourceLocation loc, MethodInfo enclosing, FieldInfo field, boolean isWrite) {
		return getFieldAccessKey(loc.getFile(), loc.getLine(), loc.getOffset(), field, isWrite);
	}

	public static String getJoinKey(SourceLocation loc) {
		return getJoinKey(loc.getFile(), loc.getLine());
	}

	public static String getStartKey(SourceLocation loc) {
		return getStartKey(loc.getFile(), loc.getLine());
	}

	public static String getWaitKey(SourceLocation loc) {
		return getWaitKey(loc.getFile(), loc.getLine());
	}

	public static String getInterruptKey(SourceLocation loc) {
		return getInterruptKey(loc.getFile(), loc.getLine());
	}
	
	public static String getInvokeKey(SourceLocation loc, MethodInfo m) {
		return getInvokeKey(loc.getFile(), loc.getLine(), loc.getOffset(), m);
	}

}
