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

import acme.util.Util;

public class MetaDataInfoPrinter implements MetaDataInfoVisitor {

	public void visit(ClassInfo x) {
		Util.log("");
		Util.log("");
		Util.log("");
		Util.logf("@%s", x.getDecorationsAsString());
		if (x.isClass()) {
			Util.logf("class %s extends %s implements %s", x.getName(), x.getSuperClass(), x.getInterfaces());
		} else { 
			Util.logf("interface %s extends %s", x.getName(), x.getInterfaces());
		}
	}

	public void visit(FieldInfo x) {
		Util.logf("    field %s %s %s%s%s @%s", x.getDescriptor(), x.getName(), x.isStatic()? "s" : "-", x.isFinal()? "f" : "-", x.isVolatile()? "v" : "-", x.getDecorationsAsString());
	}

	public void visit(MethodInfo x) {
		Util.logf("    method %s %s @%s", x.getName(), x.getDescriptor(), x.getDecorationsAsString());
	}

	protected void visitOp(OperationInfo x) {
		Util.logf("          %s  @%s", x.getKey(), x.getDecorationsAsString());
	}
	
	public void visit(AcquireInfo x) {
		visitOp(x);
	}

	public void visit(ReleaseInfo x) {
		visitOp(x);
	}

	public void visit(ArrayAccessInfo x) {
		visitOp(x);
	}

	public void visit(FieldAccessInfo x) {
		visitOp(x);
	}

	public void visit(JoinInfo x) {
		visitOp(x);
	}

	public void visit(StartInfo x) {
		visitOp(x);
	}

	public void visit(WaitInfo x) {
		visitOp(x);
	}

	public void visit(InterruptInfo x) {
		visitOp(x);
	}


	public void visit(InvokeInfo x) {
		visitOp(x);
	}
	
}
