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

import rr.loader.Loader;
import rr.loader.LoaderContext;
import rr.state.update.AbstractFieldUpdater;
import acme.util.Assert;
import acme.util.Util;

public class FieldInfo extends MetaDataInfo {
	protected final ClassInfo rrClass;
	protected final String name;
	protected final String descriptor;
	protected boolean isVolatile;
	protected boolean isStatic; 
	protected boolean isFinal; 
	protected final boolean isSynthetic;

	protected transient AbstractFieldUpdater updater = null;

	final static Vector<FieldInfo> statics = new Vector<FieldInfo>();

	public FieldInfo(int id, SourceLocation loc, ClassInfo rrClass, String name, String descriptor, boolean isSynthetic) {
		super(id, loc);
		this.rrClass = rrClass;
		this.name = name;
		this.descriptor = descriptor;
		this.isSynthetic = isSynthetic;
	} 

	public void setFlags(boolean isFinal, boolean isVolatile, boolean isStatic) {
		this.isFinal = isFinal;
		this.isVolatile = isVolatile;
		this.isStatic = isStatic;
		if (isStatic && InstrumentationFilter.shouldInstrument(rrClass) && !statics.contains(this)) {
			statics.add(this);
		}
	}

	public ClassInfo getOwner() {
		return rrClass;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isFinal() {
		return isFinal;
	}


	public boolean isSynthetic() {
		return isSynthetic;
	}


	public AbstractFieldUpdater getUpdater() {
		try {
			if (updater == null) {
				try {
					final LoaderContext loaderForClass = Loader.loaderForClass(rrClass.getName());
					AbstractFieldUpdater u = loaderForClass.getGuardStateThunkObject(rrClass.getName(), name, isStatic, isVolatile);
					setUpdater(u);
				} catch (Exception e) {
					Assert.panic(e);
				}
			}
		} catch (Throwable e) {
			Assert.panic(e);
		}
		return updater;
	}


	@Override
	protected String computeKey() {
		return MetaDataInfoKeys.getFieldKey(this.getOwner(), getName(), this.getDescriptor());
	}


	@Override
	public void accept(MetaDataInfoVisitor v) {
		v.visit(this);
	}

	public void setUpdater(AbstractFieldUpdater guardStateThunk) {
		updater = guardStateThunk;
	}

	private int offset = -1;

	/*
	 * Returns a unique offset for each field of an object.  Subclass fields
	 * will have offsets > than inherited fields.
	 * Static fields have unique offsets, also starting with 0.  
	 */
	public int getFieldOffset() {
		if (offset == -1) {
			if (!isStatic) {
				offset = this.getOwner().getOffsetOfInstanceField(this);
			} else {
				offset = statics.indexOf(this);				
			}
		} 
		return offset;
	}

}
