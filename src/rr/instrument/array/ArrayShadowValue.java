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

// based on ASM code

/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2005 INRIA, France Telecom
 * All rights reserved.  
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package rr.instrument.array;

import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.tree.analysis.Value;

/**
 * A {@link Value} that is represented by its type in a seven types type system.
 * This type system distinguishes the UNINITIALZED, INT, FLOAT, LONG, DOUBLE,
 * REFERENCE and RETURNADDRESS types.
 * 
 * @author Eric Bruneton
 */
public class ArrayShadowValue implements Value {

	public final static ArrayShadowValue SINGLE_VALUE = new ArrayShadowValue(Type.INT_TYPE, -1);
	public final static ArrayShadowValue DOUBLE_VALUE = new ArrayShadowValue(Type.LONG_TYPE, -2);
	public final static ArrayShadowValue NULL_VALUE = new ArrayShadowValue(Type.VOID_TYPE, -3);

	private Type type;  
	public final int id;  // >=0 if an target type

	public ArrayShadowValue tmp;  // used for aaload to record where the target shadow for next dim should be stored.
	
	public ArrayShadowValue(final Type type, int id) {
		this.type = type;
		this.id = id;
	}

	public Type getType() {
		return type;
	}

	public int getSize() {
		return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
	}

	@Override
	public boolean equals(final Object value) {
		return id == ((ArrayShadowValue) value).id;
	}

	@Override
	public int hashCode() {
		return type == null ? 0 : type.hashCode();
	}

	@Override
	public String toString() {
		if (this.equals(NULL_VALUE)) {
			return "[N N]";
		}
		if (type.getSort() != Type.ARRAY) {
			return "[- -]";
		} else {
			if (this.tmp == null) {
				return String.format("[%1d #]", id); 
			} else {
				return String.format("[%1d%2d]", id, tmp.id);
			}
		}
	}
	
	@Override
	public ArrayShadowValue clone() {
		if (tmp == null) {
			return this;
		} else {
			return new ArrayShadowValue(type, id);
		}
	}
}
