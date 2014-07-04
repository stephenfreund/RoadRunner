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

package rr.instrument;

import java.io.Serializable;

import rr.org.objectweb.asm.Type;

import acme.util.Assert;

import rr.meta.MethodInfo;

public class MethodContext implements Serializable {

	protected MethodInfo method;
	protected int access;
	protected int nextFreeVar;
	protected Type[] exceptions;
	protected String signature;
	 
	protected int threadStateVar = -1;
	
	public MethodContext(MethodInfo m) {
		this.method = m;
	}
	
	public MethodInfo getMethod() {
		return method;
	}

	public ClassContext getClassContext() {
		return Instrumentor.classContext.get(method.getOwner());
	}
	
	public int getAccess() {
		return access;
	}

	public void setMethod(MethodInfo method) {
		this.method = method;
	}

	public void setAccess(int access) {
		this.access = access;
	}

	public String getFileName() {
		return Instrumentor.classContext.get(method.getOwner()).getFileName();
	}
	
	public int getNextFreeVar(int size) {
		int i = nextFreeVar;
		nextFreeVar += size;
		return i;
	}

	public Type[] getExceptions() {
		return exceptions;
	}

	public String getSignature() {
		return signature;
	}

	public void setExceptions(Type[] exceptions) {
		this.exceptions = exceptions;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public int getThreadDataVar() {
		Assert.assertTrue(this.threadStateVar != -1, this.method + "");
		return this.threadStateVar;
	}
	

	public int getMaxVar() {
		return this.nextFreeVar - 1;
	}

	public void setFirstFreeVar(int maxLocals) {
		this.nextFreeVar = maxLocals;
		this.threadStateVar = this.getNextFreeVar(1);
	}
	
}
