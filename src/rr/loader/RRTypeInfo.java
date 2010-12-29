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

import java.util.Vector;

import rr.instrument.Constants;
import rr.meta.ClassInfo;
import rr.meta.FieldInfo;
import rr.meta.MetaDataInfoMaps;
import rr.meta.MethodInfo;
import acme.util.Assert;
import acme.util.collections.FilteringIterator;
import acme.util.collections.IterableIterator;

public class RRTypeInfo {

	public static MethodInfo resolveMethodDescriptor(String rrClass, String mName, String mDesc) throws MethodResolutionException {
		return resolveMethodDescriptor(MetaDataInfoMaps.getClass(rrClass), mName, mDesc);
	}

	public static MethodInfo resolveMethodDescriptor(ClassInfo rrClass, String mName, String mDesc) throws MethodResolutionException {
		if (rrClass.isSynthetic() || Constants.isSyntheticName(mName) || mName.contains("<init>")) {
			return MetaDataInfoMaps.getMethod(rrClass, mName, mDesc);
		}
		for (MethodInfo m : rrClass.getMethods()) {
			if (m.getName().equals(mName) && m.getDescriptor().equals(mDesc)) {
				return m;
			}
		}
		for (ClassInfo c: rrClass.getSuperTypes()) {
			for (MethodInfo m : c.getMethods()) {
				if (m.getName().equals(mName) && m.getDescriptor().equals(mDesc)) {
					return m;
				}
			}
		}
		
		// SNF: This code will not fail anymore.  
		// Similar as below I believe.
		// Let caller handle error instead.
		
//		Assert.fail("Cannot find method: %s.%s:%s", rrClass, mName, mDesc);
		throw new MethodResolutionException(rrClass, mName, mDesc);
	}

	public static Iterable<ClassInfo> declaringClassesForMethodDescriptor(final MethodInfo method) {
		ClassInfo owner = method.getOwner();
		return new IterableIterator<ClassInfo>(new FilteringIterator<ClassInfo>(owner.getSuperTypes().iterator()) {
			@Override
			public boolean test(ClassInfo c) {
				for (MethodInfo m : c.getMethods()) {
					if (m.getName().equals(method.getName()) && m.getDescriptor().equals(method.getDescriptor())) {
						return true;
					}
				}
				return false;
			}
		});
	}

	public static Iterable<MethodInfo> overiddenMethodsForMethodDescriptor(final MethodInfo method) {
		Vector<MethodInfo> ms = new Vector<MethodInfo>();
		for (ClassInfo c : declaringClassesForMethodDescriptor(method)) {
			ms.add(MetaDataInfoMaps.getMethod(c, method.getName(), method.getDescriptor()));
		}
		return ms;
	}

	public static FieldInfo resolveFieldDescriptorHelper(ClassInfo rrClass, String fName, String fDesc) {
		for (FieldInfo f : rrClass.getFields()) {
			if (f.getName().equals(fName)) {
				
				// SNF: This code will not fail anymore.  
				//
				// This change is necessary because I am seeing incorrect descriptors come from
				// ASM visitors:
				// class A {
				//    int x;
				// }
				// class B extends A {
				//   int f() {
				//       x = 2;
				//   }
				// }
				// The descriptor for x is (B,x,I) instead of (A,x,I) for some reason.
				
//				Assert.assertTrue(fDesc.equals(f.getDescriptor()), "Descriptors not equal for " + rrClass + "." + fName + ": " + fDesc + " and " + f.getDescriptor());
				return f;
			}
		}
		if (rrClass.getSuperClass() != null) {
			FieldInfo f = resolveFieldDescriptorHelper(rrClass.getSuperClass(), fName, fDesc);
			if (f != null) {
				return f;
			}
		}

		for (ClassInfo c : rrClass.getInterfaces()) {
			FieldInfo f = resolveFieldDescriptorHelper(c, fName, fDesc);
			if (f != null) {
				return f;
			}
		}
		return null;
	}



	public static FieldInfo resolveFieldDescriptor(ClassInfo rrClass, String fName, String fDesc) {
		FieldInfo f = resolveFieldDescriptorHelper(rrClass, fName, fDesc);
		if (f == null) {
			Assert.fail("Cannot find field: %s.%s:%s", rrClass, fName, fDesc);
		}
		return f;
	}

	public static FieldInfo resolveFieldDescriptor(String rrClass, String fName, String fDesc) {
		return resolveFieldDescriptor(MetaDataInfoMaps.getClass(rrClass), fName, fDesc);
	}
}
