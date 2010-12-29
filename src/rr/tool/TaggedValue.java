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

package rr.tool;

import acme.util.Assert;
import acme.util.Util;

/**
 * @RRExperimental
 */
public class TaggedValue implements Cloneable {

	public enum Type { BOGUS, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE, BOOLEAN, OBJECT }

	protected final boolean fixed;

	public TaggedValue() {
		this(false);
	}

	public TaggedValue(boolean fixed) {
		this.fixed = fixed;
	}

	public static final String BOGUS_VALUE = new String("BOGUS_VALUE");

	protected Type type = Type.BOGUS;

	//	protected byte byteValue;
	//	protected char charValue;
	//	protected short shortValue;
	protected int intValue;
	protected long longValue;
	//	protected float floatValue;
	protected double doubleValue;
	protected boolean booleanValue;
	protected Object objectValue;

	public char getcharValue() {
		Assert.assertTrue(type == Type.CHAR);
		return (char)intValue;
	}
	public short getshortValue() {
		Assert.assertTrue(type == Type.SHORT);
		return (short)intValue;
	}
	public int getintValue() {
		Assert.assertTrue(type == Type.INT);
		return intValue;
	}
	public long getlongValue() {
		Assert.assertTrue(type == Type.LONG);
		return longValue;
	}
	public float getfloatValue() {
		Assert.assertTrue(type == Type.FLOAT);
		return (float)doubleValue;
	}
	public double getdoubleValue() {
		Assert.assertTrue(type == Type.DOUBLE);
		return doubleValue;
	}
	public byte getbyteValue() {
		Assert.assertTrue(type == Type.BYTE);
		return (byte)intValue;
	}
	public boolean getbooleanValue() {
		Assert.assertTrue(type == Type.BOOLEAN);
		return booleanValue;
	}
	public Object getObjectValue() {
		Assert.assertTrue(type == Type.OBJECT);
		return objectValue;
	}
	public void setValue(char charValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.CHAR;
		this.intValue = charValue;
	}
	public void setValue(short shortValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.SHORT;
		this.intValue = shortValue;
	}
	public void setValue(int intValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.INT;
		this.intValue = intValue;
	}
	public void setValue(long longValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.LONG;
		this.longValue = longValue;
	}
	public void setValue(float floatValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.FLOAT;
		this.doubleValue = floatValue;
	}
	public void setValue(double doubleValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.DOUBLE;
		this.doubleValue = doubleValue;
	}
	public void setValue(byte byteValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.BYTE;
		this.intValue = byteValue;
	}
	public void setValue(boolean booleanValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.BOOLEAN;
		this.booleanValue = booleanValue;
	}
	public void setValue(Object objectValue) {
		Assert.assertTrue(!this.fixed);
		this.type = Type.OBJECT;
		this.objectValue = objectValue;
	}

	public boolean isEmpty() {
		return type == Type.BOGUS;
	}

	public Type getType() {
		return type;
	}

	public void clear() {
		Assert.assertTrue(!this.fixed);
		this.type = Type.BOGUS;
	}

	public void copyFrom(TaggedValue other) {
		Assert.assertTrue(!this.fixed);
		this.type = other.type;
		switch(this.type) {
			case BOGUS: break;
			case CHAR:  
			case SHORT:	
			case INT: 	this.intValue = other.intValue; break;
			case LONG:	this.longValue = other.longValue; break;
			case FLOAT:	
			case DOUBLE:this.doubleValue = other.doubleValue; break;
			case BYTE:	this.intValue = other.intValue; break;
			case BOOLEAN:this.booleanValue = other.booleanValue; break;
			case OBJECT:this.objectValue = other.objectValue; break;
		}
	}

	@Override
	public boolean equals(Object o) {
		TaggedValue other = (TaggedValue)o;
		if (other == null) return false;
		if (this.type != other.type) return false;
		switch(this.type) {
			case BOGUS: return true;
			case CHAR:  
			case SHORT:	
			case INT: 	return this.intValue == other.intValue; 
			case LONG:	return this.longValue == other.longValue; 
			case FLOAT:	 
			case DOUBLE:return this.doubleValue == other.doubleValue; 
			case BYTE:	return this.intValue == other.intValue; 
			case BOOLEAN:return this.booleanValue == other.booleanValue; 
			case OBJECT:return this.objectValue == other.objectValue; 
		}
		return false;
	}

	public Object getBoxedValue() {
		switch(this.type) {
			case BOGUS: return BOGUS_VALUE;
			case CHAR:  return Character.valueOf((char)intValue);
			case SHORT:	return Short.valueOf((short)intValue);
			case INT: 	return Integer.valueOf(intValue);
			case LONG:	return Long.valueOf(longValue);
			case FLOAT:	return Float.valueOf((float)doubleValue);
			case DOUBLE: return Double.valueOf(doubleValue);
			case BYTE:	return Byte.valueOf((byte)intValue);
			case BOOLEAN:return Boolean.valueOf(booleanValue);
			case OBJECT: return objectValue;
		}
		return null;
	}

	@Override
	public String toString() {
		return type + ":" + Util.boxedValueToValueString(getBoxedValue());
	}



	////////////

	static TaggedValue internedBogus = new TaggedValue(true);
	static TaggedValue internedNull = new TaggedValue(true);
	static TaggedValue internedTrue = new TaggedValue(true);
	static TaggedValue internedFalse = new TaggedValue(true);
	static TaggedValue[] internedBytes = new TaggedValue[256]; 
	static TaggedValue[] internedChars = new TaggedValue[256]; 
	static TaggedValue[] internedShorts = new TaggedValue[256]; 
	static TaggedValue[] internedInts = new TaggedValue[2048]; 

	static {
		internedBogus.objectValue = BOGUS_VALUE;
		internedBogus.type = Type.BOGUS;

		internedNull.objectValue = null;
		internedNull.type = Type.OBJECT;

		internedTrue.booleanValue = true;
		internedTrue.type = Type.BOOLEAN;

		internedFalse.booleanValue = false;
		internedFalse.type = Type.BOOLEAN;

	}

	public static TaggedValue initZero(Type t) {
		TaggedValue v = new TaggedValue();
		switch(t) {
			case BOGUS: break;
			case CHAR:  
				v.setValue((char)0); break;
			case SHORT:	
				v.setValue((short)0); break;
			case INT: 	
				v.setValue(0); break;
			case LONG:	
				v.setValue((long)0); break;
			case FLOAT:	
				v.setValue((float)0); break;
			case DOUBLE:
				v.setValue((double)0); break;
			case BYTE:	
				v.setValue((byte)0); break;
			case BOOLEAN:
				v.setValue(false); break;
			case OBJECT:
				v.setValue(null); break;
		}
		return v;
	}


	public TaggedValue makeFinalCopy() {
		//		if (true) {
		//			try {
		//				return (TaggedValue)this.clone();
		//			} catch (CloneNotSupportedException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
		//		}
		TaggedValue v = null;

		if (this.fixed) {
			v = this;
		}


		switch(this.type) {
			case BOGUS: {
				v = internedBogus;
				break;
			}
			case CHAR:  {
				final int x = this.getcharValue();
				if ((x & ~255) == 0) {
					v = internedChars[x];
					if (v == null) {
						v = internedChars[x] = new TaggedValue(true);
						v.intValue = this.intValue;
						v.type = this.type;
					}
				} else {
					v = new TaggedValue();
					v.copyFrom(this);
				}
				break;
			}
			case SHORT: {
				final int x = this.getshortValue();
				if ((x & ~255) == 0) {
					v = internedShorts[x];
					if (v == null) {
						v = internedShorts[x] = new TaggedValue(true);
						v.intValue = this.intValue;
						v.type = this.type;
					}
				} else {
					v = new TaggedValue();
					v.copyFrom(this);
				}
				break;
			}	
			case INT: 	{
				final int x = this.getintValue();
				if ((x & ~1023) == 0) {
					v = internedInts[x];
					if (v == null) {
						v = internedInts[x] = new TaggedValue(true);
						v.intValue = this.intValue;
						v.type = this.type;
					}
				} else {
					v = new TaggedValue();
					v.copyFrom(this);
				}
				break;
			} 
			case BYTE:  {
				final int x = this.getbyteValue();
				v = internedBytes[x+127];
				if (v == null) {
					v = internedBytes[x+127] = new TaggedValue(true);
					v.intValue = this.intValue;
					v.type = this.type;
				}
				break;
			}
			case BOOLEAN: {
				v = Boolean.valueOf(booleanValue) ? internedTrue : internedFalse;
				break;
			}
			case LONG:	{
				v = new TaggedValue(true);
				v.longValue = this.longValue;
				v.type = this.type;
				break;
			}
			case FLOAT:	
			case DOUBLE: { 
				v = new TaggedValue(true);
				v.doubleValue = this.doubleValue;
				v.type = this.type;
				break;
			}
			case OBJECT: {
				v = new TaggedValue(true);
				v.objectValue = this.objectValue;
				v.type = this.type;
				break;
			}
		}
		return v;

	}

}
