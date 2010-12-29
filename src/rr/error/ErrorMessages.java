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

package rr.error;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import rr.meta.MetaDataInfoMaps;
import rr.meta.AcquireInfo;
import rr.meta.ArrayAccessInfo;
import rr.meta.FieldAccessInfo;
import rr.meta.FieldInfo;
import rr.meta.MethodInfo;
import acme.util.io.XMLWriter;

/**
 * Helper routines for managing error messages on the most common
 * syntactic categories.  The categories are all the same -- see methods
 * for a description.
 */
public class ErrorMessages {

	static private Vector<ErrorMessage<MethodInfo>> methodErrorMessages = new Vector<ErrorMessage<MethodInfo>>();
	static private Vector<ErrorMessage<FieldInfo>> fieldErrorMessages = new Vector<ErrorMessage<FieldInfo>>();
	static private Vector<ErrorMessage<FieldAccessInfo>> fieldAccessErrorMessages = new Vector<ErrorMessage<FieldAccessInfo>>();
	static private Vector<ErrorMessage<ArrayAccessInfo>> arrayErrorMessages = new Vector<ErrorMessage<ArrayAccessInfo>>();
	static private Vector<ErrorMessage<AcquireInfo>> lockErrorMessages = new Vector<ErrorMessage<AcquireInfo>>();
	
	/** 
	 * Create a new type of error message for reporting errors on methods.
	 */
	static public ErrorMessage<MethodInfo> makeMethodErrorMessage(String type) {
		ErrorMessage<MethodInfo> ead = new ErrorMessage<MethodInfo>(type);
		methodErrorMessages.add(ead);
		return ead;
	}

	/** 
	 * Return all error message reporters installed for methods.
	 */
	static public Iterable<ErrorMessage<MethodInfo>> methodErrorMessages() {
		return methodErrorMessages;
	}

	/** 
	 * Number of errors on a given method.
	 */
	static public int numErrorsOnMethod(MethodInfo ad) {
		int count = 0;
		for (ErrorMessage<MethodInfo> ead : methodErrorMessages) {
			count += ead.count(ad);
		}
		return count;
	}
	
	/**
	 * Convert method error info to XML.
	 */
	public static void xmlErrorsByMethod(XMLWriter xml) {

		xml.push("methods");
		for (MethodInfo ad: MetaDataInfoMaps.getMethods()) {
			int errors = ErrorMessages.numErrorsOnMethod(ad);
			if (errors > 0) {
				xml.push("method");
				xml.print("name", ad.toString() + "(" + ad.getLoc() + ")");
				for (ErrorMessage<MethodInfo> e : ErrorMessages.methodErrorMessages()) {
					if (e.count(ad) > 0) {
						xml.printInsideScope("error", "name", e, "count", e.count(ad));
					}
				}
				xml.pop();
			}
		}	
		xml.pop();
	}
	
	
	
	static public ErrorMessage<FieldInfo> makeFieldErrorMessage(String type) {
		ErrorMessage<FieldInfo> ead = new ErrorMessage<FieldInfo>(type);
		fieldErrorMessages.add(ead);
		return ead;
	}

	static public Iterable<ErrorMessage<FieldInfo>> fieldErrorMessages() {
		return fieldErrorMessages;
	}

	static public int numErrorsOnField(FieldInfo fd) {
		int count = 0;
		for (ErrorMessage<FieldInfo> ead : fieldErrorMessages) {
			count += ead.count(fd);
		}
		return count;
	}

	public static void xmlErrorsByField(XMLWriter xml) {

		xml.push("fields");
		for (FieldInfo fd: MetaDataInfoMaps.getFields()) {
			int errors = ErrorMessages.numErrorsOnField(fd);
			if (errors > 0) {
				xml.push("field");
				xml.print("name", fd.toString());
				for (ErrorMessage<FieldInfo> e : ErrorMessages.fieldErrorMessages()) {
					if (e.count(fd) > 0) {
						xml.printInsideScope("error", "name", e, "count", e.count(fd));
					}
				}
				xml.pop();
			}
		}	
		xml.pop();
	}
	

	static public ErrorMessage<FieldAccessInfo> makeFieldAccessErrorMessage(String type) {
		ErrorMessage<FieldAccessInfo> ead = new ErrorMessage<FieldAccessInfo>(type);
		fieldAccessErrorMessages.add(ead);
		return ead;
	}

	static public Iterable<ErrorMessage<FieldAccessInfo>> fieldAccessErrorMessages() {
		return fieldAccessErrorMessages;
	}

	static public int numErrorsOnFieldAccess(FieldAccessInfo fd) {
		int count = 0;
		for (ErrorMessage<FieldAccessInfo> ead : fieldAccessErrorMessages) {
			count += ead.count(fd);
		}
		return count;
	}
	
	public static void xmlErrorsByFieldAccess(XMLWriter xml) {

		xml.push("fieldAccesses");
		for (FieldAccessInfo fa: MetaDataInfoMaps.getFieldAccesses()) {
			int errors = ErrorMessages.numErrorsOnFieldAccess(fa);
			if (errors > 0) {
				xml.push("fieldAccess");
				xml.print("name", fa.toString());
				for (ErrorMessage<FieldAccessInfo> e : ErrorMessages.fieldAccessErrorMessages()) {
					if (e.count(fa) > 0) {
						xml.printInsideScope("error", "name", e, "count", e.count(fa));
					}
				}
				xml.pop();
			}
		}	
		xml.pop();
	}
	
	static public ErrorMessage<ArrayAccessInfo> makeArrayErrorMessage(String type) {
		ErrorMessage<ArrayAccessInfo> ead = new ErrorMessage<ArrayAccessInfo>(type);
		arrayErrorMessages.add(ead);
		return ead;
	}

	static public Iterable<ErrorMessage<ArrayAccessInfo>> arrayErrorMessages() {
		return arrayErrorMessages;
	}

	static public int numErrorsOnArray(ArrayAccessInfo aad) {
		int count = 0;
		for (ErrorMessage<ArrayAccessInfo> ead : arrayErrorMessages) {
			count += ead.count(aad);
		}
		return count;
	}
	
	public static void xmlErrorsByArray(XMLWriter xml) {

		xml.push("arrays");
		for (ArrayAccessInfo aa: MetaDataInfoMaps.getArrayAccesses()) {
			int errors = ErrorMessages.numErrorsOnArray(aa);
			if (errors > 0) {
				xml.push("target");
				xml.print("location", aa.toString());
				for (ErrorMessage<ArrayAccessInfo> e : ErrorMessages.arrayErrorMessages()) {
					if (e.count(aa) > 0) {
						xml.printInsideScope("error", "name", e, "count", e.count(aa));
					}
				}
				xml.pop();
			}
		}	
		xml.pop();
	}

	
	static public ErrorMessage<AcquireInfo> makeLockErrorMessage(String type) {
		ErrorMessage<AcquireInfo> ead = new ErrorMessage<AcquireInfo>(type);
		lockErrorMessages.add(ead);
		return ead;
	}

	static public Iterable<ErrorMessage<AcquireInfo>> lockErrorMessages() {
		return lockErrorMessages;
	}

	static public int numErrorsOnLock(AcquireInfo aad) {
		int count = 0;
		for (ErrorMessage<AcquireInfo> ead : lockErrorMessages) {
			count += ead.count(aad);
		}
		return count;
	}

	public static void xmlErrorsByLock(XMLWriter xml) {

		xml.push("locks");
		for (AcquireInfo ad: MetaDataInfoMaps.getAcquires()) {
			int errors = ErrorMessages.numErrorsOnLock(ad);
			if (errors > 0) {
				xml.push("lock");
				xml.print("name", ad.toString() + "(" + ad.getLoc() + ")");
				for (ErrorMessage<AcquireInfo> e : ErrorMessages.lockErrorMessages()) {
					if (e.count(ad) > 0) {
						xml.printInsideScope("error", "name", e, "count", e.count(ad));
					}
				}
				xml.pop();
			}
		}	
		xml.pop();
	}
	
	
	/**
	 * Convert all error info to XML.
	 */
	public static void xmlErrorsByErrorType(XMLWriter xml) {
		HashMap<String,Integer> errMap = new HashMap<String,Integer>();

		for (FieldInfo fd : MetaDataInfoMaps.getFields()) {
			int errors = ErrorMessages.numErrorsOnField(fd);
			if (errors > 0) {
				for (ErrorMessage<FieldInfo> e : ErrorMessages.fieldErrorMessages()) {
					if (e.count(fd) > 0) {
						Integer count = errMap.get(e.toString());
						if (count == null) count = new Integer(0);
						count = count + e.count(fd);
						errMap.put(e.toString(), count);
					}
				}
			}
		}
		for (ArrayAccessInfo aad : MetaDataInfoMaps.getArrayAccesses()) {
			int errors = ErrorMessages.numErrorsOnArray(aad);
			if (errors > 0) {
				for (ErrorMessage<ArrayAccessInfo> e : ErrorMessages.arrayErrorMessages()) {
					if (e.count(aad) > 0) {
						Integer count = errMap.get(e.toString());
						if (count == null) count = new Integer(0);
						count = count + e.count(aad);
						errMap.put(e.toString(), count);
					}
				}
			}
		}
		for (AcquireInfo a : MetaDataInfoMaps.getAcquires()) {
			int errors = ErrorMessages.numErrorsOnLock(a);
			if (errors > 0) {
				for (ErrorMessage<AcquireInfo> e : lockErrorMessages) {
					if (e.count(a) > 0) {
						Integer count = errMap.get(e.toString());
						if (count == null) count = new Integer(0);
						count = count + e.count(a);
						errMap.put(e.toString(), count);
					}
				}
			}
		}
		for (FieldAccessInfo fa : MetaDataInfoMaps.getFieldAccesses()) {
			int errors = ErrorMessages.numErrorsOnFieldAccess(fa);
			if (errors > 0) {
				for (ErrorMessage<FieldAccessInfo> e : ErrorMessages.fieldAccessErrorMessages()) {
					if (e.count(fa) > 0) {
						Integer count = errMap.get(e.toString());
						if (count == null) count = new Integer(0);
						count = count + e.count(fa);
						errMap.put(e.toString(), count);
					}
				}
			}
		}
		for (MethodInfo md : MetaDataInfoMaps.getMethods()) {
			int errors = ErrorMessages.numErrorsOnMethod(md);
			if (errors > 0) {
				for (ErrorMessage<MethodInfo> e : ErrorMessages.methodErrorMessages()) {
					if (e.count(md) > 0) {
						Integer count = errMap.get(e.toString());
						if (count == null) count = new Integer(0);
						count = count + e.count(md);
						errMap.put(e.toString(), count);
					}
				}
			}
		}

		xml.push("errorCountPerErrorType");
		Iterator<String> i = errMap.keySet().iterator();
		while (i.hasNext()) {
			String errType = i.next();
			Integer count = errMap.get(errType);
			xml.printInsideScope("errorType", "name", errType, "count", count);
		}
		xml.pop();
	}
	
}
