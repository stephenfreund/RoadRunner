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

import java.util.List;

import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.tree.AbstractInsnNode;
import rr.org.objectweb.asm.tree.FieldInsnNode;
import rr.org.objectweb.asm.tree.IntInsnNode;
import rr.org.objectweb.asm.tree.LdcInsnNode;
import rr.org.objectweb.asm.tree.MethodInsnNode;
import rr.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import rr.org.objectweb.asm.tree.TypeInsnNode;
import rr.org.objectweb.asm.tree.analysis.AnalyzerException;
import rr.org.objectweb.asm.tree.analysis.Interpreter;
import rr.org.objectweb.asm.tree.analysis.Value;

import acme.util.Util;


/**
 * An {@link Interpreter} for {@link ArrayShadowValue} values.
 * 
 * @author Eric Bruneton
 * @author Bing Ran
 */
public class ArrayShadowInterpreter extends Interpreter<ArrayShadowValue> implements Opcodes {
	
	protected ArrayShadowInterpreter() {
		super(ASM5);
	}


	private int counter = 0;
	
    public ArrayShadowValue newValue(final Type type) {
        if (type == null) {
            return ArrayShadowValue.SINGLE_VALUE;
        }
        switch (type.getSort()) {
            case Type.VOID:
                return null;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return ArrayShadowValue.SINGLE_VALUE;
            case Type.FLOAT:
                return ArrayShadowValue.SINGLE_VALUE;
            case Type.LONG:
                return ArrayShadowValue.DOUBLE_VALUE;
            case Type.DOUBLE:
                return ArrayShadowValue.DOUBLE_VALUE;
            case Type.OBJECT:
                return ArrayShadowValue.SINGLE_VALUE;
            case Type.ARRAY:
            	return new ArrayShadowValue(type, counter++);
            default:
                throw new Error("Internal error");
        }
    }

    public ArrayShadowValue newOperation(final AbstractInsnNode insn) {
        switch (insn.getOpcode()) {
        	case ACONST_NULL:
                return ArrayShadowValue.NULL_VALUE;
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                return ArrayShadowValue.SINGLE_VALUE;
            case LCONST_0:
            case LCONST_1:
                return ArrayShadowValue.DOUBLE_VALUE;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                return ArrayShadowValue.SINGLE_VALUE;
            case DCONST_0:
            case DCONST_1:
                return ArrayShadowValue.DOUBLE_VALUE;
            case BIPUSH:
            case SIPUSH:
                return ArrayShadowValue.SINGLE_VALUE;
            case LDC:
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof Integer) {
                    return ArrayShadowValue.SINGLE_VALUE;
                } else if (cst instanceof Float) {
                    return ArrayShadowValue.SINGLE_VALUE;
                } else if (cst instanceof Long) {
                    return ArrayShadowValue.DOUBLE_VALUE;
                } else if (cst instanceof Double) {
                    return ArrayShadowValue.DOUBLE_VALUE;
                } else {
                    return ArrayShadowValue.SINGLE_VALUE;
                }
            case JSR:
                return ArrayShadowValue.SINGLE_VALUE;
            case GETSTATIC:
                return newValue(Type.getType(((FieldInsnNode) insn).desc));
            case NEW:
                return ArrayShadowValue.SINGLE_VALUE;
            default:
                throw new Error("Internal error.");
        }
    }

    public ArrayShadowValue copyOperation(final AbstractInsnNode insn, final ArrayShadowValue value)
            throws AnalyzerException
    {
        return value;
    }

    public ArrayShadowValue unaryOperation(final AbstractInsnNode insn, final ArrayShadowValue value)
            throws AnalyzerException
    {
        switch (insn.getOpcode()) {
            case INEG:
            case IINC:
            case L2I:
            case F2I:
            case D2I:
            case I2B:
            case I2C:
            case I2S:
                return ArrayShadowValue.SINGLE_VALUE;
            case FNEG:
            case I2F:
            case L2F:
            case D2F:
                return ArrayShadowValue.SINGLE_VALUE;
            case LNEG:
            case I2L:
            case F2L:
            case D2L:
                return ArrayShadowValue.DOUBLE_VALUE;
            case DNEG:
            case I2D:
            case L2D:
            case F2D:
                return ArrayShadowValue.DOUBLE_VALUE;
            case IFEQ:
            case IFNE:
            case IFLT:
            case IFGE:
            case IFGT:
            case IFLE:
            case TABLESWITCH:
            case LOOKUPSWITCH:
            case IRETURN:
            case LRETURN:
            case FRETURN:
            case DRETURN:
            case ARETURN:
            case PUTSTATIC:
                return null;
            case GETFIELD:
            	//Util.log(Type.getType(((FieldInsnNode) insn).desc) + " <-- ");
            	ArrayShadowValue v = newValue(Type.getType(((FieldInsnNode) insn).desc));
                // Util.log("" + v);
                return v;
            case NEWARRAY:
                switch (((IntInsnNode) insn).operand) {
                    case T_BOOLEAN:
                        return newValue(Type.getType("[Z"));
                    case T_CHAR:
                        return newValue(Type.getType("[C"));
                    case T_BYTE:
                        return newValue(Type.getType("[B"));
                    case T_SHORT:
                        return newValue(Type.getType("[S"));
                    case T_INT:
                        return newValue(Type.getType("[I"));
                    case T_FLOAT:
                        return newValue(Type.getType("[F"));
                    case T_DOUBLE:
                        return newValue(Type.getType("[D"));
                    case T_LONG:
                        return newValue(Type.getType("[J"));
                    default:
                        throw new AnalyzerException(insn, "Invalid target type");
                }
            case ANEWARRAY: {
                String desc = ((TypeInsnNode) insn).desc;
                if (desc.charAt(0) == '[') {
                    return newValue(Type.getType("[" + desc));
                } else {
                    return newValue(Type.getType("[L" + desc + ";"));
                }
            }
            case ARRAYLENGTH:
                return ArrayShadowValue.SINGLE_VALUE;
            case ATHROW:
                return null;
            case CHECKCAST: {
                String desc = ((TypeInsnNode) insn).desc;
                if (desc.charAt(0) == '[') {
                    return newValue(Type.getType(desc)); 
                } else {
                	return value;
                }
            }
            case INSTANCEOF:
                return ArrayShadowValue.SINGLE_VALUE;
            case MONITORENTER:
            case MONITOREXIT:
            case IFNULL:
            case IFNONNULL:
                return null;
            default:
                throw new Error("Internal error.");
        }
    }

    public ArrayShadowValue binaryOperation(
        final AbstractInsnNode insn,
        final ArrayShadowValue value1,
        final ArrayShadowValue value2) throws AnalyzerException
    {
        switch (insn.getOpcode()) {
            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
            case IADD:
            case ISUB:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
                return ArrayShadowValue.SINGLE_VALUE;
            case FALOAD:
            case FADD:
            case FSUB:
            case FMUL:
            case FDIV:
            case FREM:
                return ArrayShadowValue.SINGLE_VALUE;
            case LALOAD:
            case LADD:
            case LSUB:
            case LMUL:
            case LDIV:
            case LREM:
            case LSHL:
            case LSHR:
            case LUSHR:
            case LAND:
            case LOR:
            case LXOR:
                return ArrayShadowValue.DOUBLE_VALUE;
            case DALOAD:
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                return ArrayShadowValue.DOUBLE_VALUE;
            case AALOAD: {
                Type t;
                if (value1.equals(ArrayShadowValue.NULL_VALUE)) {
                    Util.log("Possibly NULL array!!!");
                	t = Type.getType("[Ljava/lang/Object;");
                } else {
                	t = ((ArrayShadowValue)value1).getType();
                }
                ArrayShadowValue v = (ArrayShadowValue) newValue(Type.getType(t.getDescriptor().substring(1)));
                ((ArrayShadowValue)value1).tmp = v;
                return v;
            }
            case LCMP:
            case FCMPL:
            case FCMPG:
            case DCMPL:
            case DCMPG:
                return ArrayShadowValue.SINGLE_VALUE;
            case IF_ICMPEQ:
            case IF_ICMPNE:
            case IF_ICMPLT:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ACMPEQ:
            case IF_ACMPNE:
            case PUTFIELD:
                return null;
            default:
                throw new Error("Internal error.");
        }
    }

    public ArrayShadowValue ternaryOperation(
        final AbstractInsnNode insn,
        final ArrayShadowValue value1,
        final ArrayShadowValue value2,
        final ArrayShadowValue value3) throws AnalyzerException
    {
        return null;
    }

    public ArrayShadowValue naryOperation(final AbstractInsnNode insn, final List values)
            throws AnalyzerException
    {
        if (insn.getOpcode() == MULTIANEWARRAY) {
            return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc));
        } else {
            return newValue(Type.getReturnType(((MethodInsnNode) insn).desc));
        }
    }
 
    
    public ArrayShadowValue merge(final ArrayShadowValue v, final ArrayShadowValue w) {
        if (!v.equals(w)) {
        	ArrayShadowValue vv = (ArrayShadowValue)v;
        	ArrayShadowValue ww = (ArrayShadowValue)w;
        	
        	if (vv.equals(ArrayShadowValue.NULL_VALUE)) {
        		return ww;
        	}
        	if (ww.equals(ArrayShadowValue.NULL_VALUE)) {
        		return vv;
        	}
        	
        	// same type, so if array, keep -- the merge of loaded info sets will force a reload...
        	if (vv.getType().equals(ww.getType())) {
        		return vv;
        	} else {
        		// We have two arrays, but they are of different type, so... create a new array
        		// id and force a load since it will be fresh...
        		if (vv.id > -1 && ww.id > -1) {
        			return newValue(Type.getType(Object[].class));
        		} else {
        			return ArrayShadowValue.SINGLE_VALUE;
        		}
        	}
        } else {
        	return v;
        }
    }

	@Override
	public void returnOperation(AbstractInsnNode insn, ArrayShadowValue value,
			ArrayShadowValue expected) throws AnalyzerException {
	}


}
