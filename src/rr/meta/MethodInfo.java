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

import java.util.Vector;

import acme.util.Assert;
import acme.util.StackDump;
import acme.util.Util;

public class MethodInfo extends MetaDataInfo {

	protected final ClassInfo owner;
	protected final String name;
	protected final String desc;

	protected boolean isStatic;
	protected boolean isNative;
	protected boolean isSynchronized;
	protected boolean flagsSet = false;
	
	protected final Vector<OperationInfo> ops = new Vector<OperationInfo>();
	protected final boolean isSynthetic;
	
	public MethodInfo(int id, SourceLocation loc, ClassInfo type, String name, String descriptor, boolean isSynthetic) {
		super(id, loc);
		this.owner = type;
		this.name = name;
		this.desc = descriptor;
		this.isSynthetic = isSynthetic;
		if (name.contains("<")) {
			this.setFlags(name.contains("<clinit>"), false, false);
		}
	} 

	@Override
	protected String computeKey() {
		return MetaDataInfoKeys.getMethodKey(getOwner(), getName(), getDescriptor());
	}

	public void setFlags(boolean isStatic, boolean isNative, boolean isSynchronized) {
		if (flagsSet) { 
			Assert.assertTrue(isStatic == this.isStatic, this + " Static set twice: tools.internal = " + this.isStatic); 
			Assert.assertTrue(isNative == this.isNative, this + " Native set twice: tools.internal = " + this.isNative); 
			Assert.assertTrue(isSynchronized == this.isSynchronized, this + " Synchronized set twice: tools.internal = " + this.isSynchronized); 
		}
		this.isStatic = isStatic;
		this.isNative = isNative;
		this.isSynchronized = isSynchronized;
		flagsSet = true;
	}

	public String toSimpleName() {
		int lastSemi = getName().lastIndexOf('.');
		int oParen = getName().lastIndexOf('('); 
		if (oParen == -1) oParen = getName().length();
		return getOwner().getName() + "." + getName().substring(lastSemi > -1 ? lastSemi + 1 : 0, oParen);
	}


	public ClassInfo getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return desc;
	}

	public boolean isStatic() {
		Assert.assertTrue(flagsSet, "No flag set: " + this);
		return this.isStatic;
	}

	public void addOp(OperationInfo op) {
		ops.add(op);
	}

	public Iterable<OperationInfo> getOps() {
		return ops;
	}  

	@Override
	public void accept(MetaDataInfoVisitor v) {
		v.visit(this);
		for (OperationInfo op : ops) {
			op.accept(v);
		}
	}

	public boolean isSynthetic() {
		return isSynthetic;
	}

	public boolean isNative() {
		Assert.assertTrue(flagsSet, "No flag set: " + this);
		return isNative;
	}


	public boolean isSynchronized() {
		Assert.assertTrue(flagsSet, "No flag set: " + this);
		return isSynchronized;
	}
	
	public void setFlags(MethodInfo method) {
		this.setFlags(method.isStatic, method.isNative, method.isSynchronized);
	}


}
