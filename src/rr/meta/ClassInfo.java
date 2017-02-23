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

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import acme.util.Assert;
import acme.util.Util;

public class ClassInfo extends MetaDataInfo implements Comparable<ClassInfo> {

	public static enum State { FRESH, IN_PRELOAD, PRELOADED, COMPLETE }

	protected State state;
	protected boolean isClass;
	protected final boolean isSynthetic;
	protected final String name;
	protected ClassInfo superClass;
	protected final Vector<FieldInfo> fields = new Vector<FieldInfo>();
	protected final Vector<ClassInfo> interfaces = new Vector<ClassInfo>();
	protected final Vector<MethodInfo> methods = new Vector<MethodInfo>();

	protected volatile Vector<FieldInfo> instanceFields;


	public ClassInfo(int id, SourceLocation loc, String name, boolean isSynthetic) {
		super(id, loc); 
		Assert.assertTrue(!name.contains("."), "Bad Class: " + name);
		this.name = name;
		this.setState(State.FRESH);
		this.isSynthetic = isSynthetic;
	}

	protected void assertStateAtLeast(State s, String info) {
		if (!stateAtLeast(s)) {
			Assert.fail("State for " + this + " is " + getState() + " but must be atleast " + (s) + ". " + info);
		}
	}

	protected void assertStateAtMost(State s, String info) {
		if (!stateAtMost(s)) {
			Assert.fail("State for " + this + " is " + getState() + " but must be atmost " + (s) + ". " + info);
		}
	}

	protected void assertStateIs(State s, String info) {
		if (!stateIs(s)) {
			Assert.fail("State for " + this + " is " + getState() + " but must be " + (s) + ". " + info);
		}
	}


	protected void assertStateAtLeast(State s) {
		assertStateAtLeast(s, "");
	}

	protected void assertStateAtMost(State s) {
		assertStateAtMost(s, "");
	}

	protected void assertStateIs(State s) {
		assertStateIs(s, "");
	}

	public boolean stateAtLeast(State s) {
		return (this.getState().compareTo(s) >= 0);
	}

	public boolean stateAtMost(State s) {
		return (this.getState().compareTo(s) <= 0);
	}  

	public boolean stateIs(State s) {
		return (this.getState().compareTo(s) == 0);
	}

	public void addField(FieldInfo x) {
		if (superClass != null) {
			superClass.assertStateAtLeast(State.PRELOADED);
		}
		if (!fields.contains(x)) {
			fields.add(x);
		}
	}

	public void setSuperClass(ClassInfo superClass) {
		if (this.superClass != superClass) {
			Assert.assertTrue(instanceFields == null);
			assertStateAtMost(State.IN_PRELOAD, "add super " + superClass);
		}
		superClass.assertStateAtLeast(State.PRELOADED);
		this.isClass = true;
		this.superClass = superClass;		
	}

	public ClassInfo getSuperClass() {
		assertStateAtLeast(State.PRELOADED);
		return superClass;
	}

	public Vector<FieldInfo> getFields() {
		assertStateAtLeast(State.PRELOADED);
		return fields;
	}

	@Override
	protected String computeKey() {
		return MetaDataInfoKeys.getClassKey(this.getName());
	}

	@Override
	public void accept(MetaDataInfoVisitor v) {
		assertStateIs(State.COMPLETE);

		v.visit(this);
		for (FieldInfo f : fields) {
			f.accept(v);
		}
		for (MethodInfo m : methods) {
			m.accept(v);
		}
	}

	public void setIsClass(boolean isClass) {
		assertStateAtMost(State.IN_PRELOAD);
		this.isClass = isClass;
	}

	public boolean isClass() {
		assertStateAtLeast(State.PRELOADED);
		return isClass;
	}

	private void addAllSuperTypes(ClassInfo type, Set<ClassInfo> v) {
		if (type == null) return;
		v.add(type);
		addAllSuperTypes(type.getSuperClass(), v);
		for (ClassInfo c : type.getInterfaces()) {
			addAllSuperTypes(c, v);
		}
	}

	private Set<ClassInfo> supers;
	public Set<ClassInfo> getSuperTypes() {
		assertStateAtLeast(State.PRELOADED);
		if (supers == null) {
			supers = new HashSet<ClassInfo>();
			addAllSuperTypes(this, supers);
		}
		return supers;
	}

	public State getState() {
		return state;
	}

	protected String instanceLayout() {
		String r = "";
		for (FieldInfo x : getInstanceFields()) {
			r += " " + x.getName();
		}
		return r;
	}

	public void setState(State state) {
		this.state = state;
	}

	public void addInterface(ClassInfo i) {
		if (!interfaces.contains(i)) {
			assertStateAtMost(State.IN_PRELOAD);
			interfaces.add(i);	
		}
	}

	public void addMethod(MethodInfo x) {
		if (!methods.contains(x)) {
			methods.add(x); 
		} 
	}

	public Vector<ClassInfo> getInterfaces() {
		assertStateAtLeast(State.PRELOADED);
		return interfaces;
	}

	public Vector<MethodInfo> getMethods() {
		assertStateAtLeast(State.PRELOADED);
		return methods;
	}

	public String getName() {
		return name;
	}

	public boolean isSynthetic() {
		return this.isSynthetic;
	}

	public int compareTo(ClassInfo o) {
		return getName().compareTo(((ClassInfo)o).getName());
	}

	protected void makeFieldList() {
		if (instanceFields == null) {
			synchronized(this) {
				if (instanceFields == null) {
					Vector<FieldInfo> tmpFields;
					if (this.superClass != null) {
						superClass.makeFieldList();
						tmpFields = new Vector<FieldInfo>(superClass.instanceFields);
					} else {
						tmpFields = new Vector<FieldInfo>();
					}
					if (InstrumentationFilter.shouldInstrument(this)) {
						for (FieldInfo x : fields) {
							if (!x.isStatic() && !x.isFinal()) {
								tmpFields.add(x);
							}
						}
					}
					instanceFields = tmpFields;
				}
			}
		}
	}

	public Vector<FieldInfo> getInstanceFields() {
		assertStateAtLeast(State.PRELOADED);
		makeFieldList();
		return instanceFields;
	}

	public int getOffsetOfInstanceField(FieldInfo x) {
		makeFieldList();
		int i = instanceFields.indexOf(x);
		return i;
	}

}