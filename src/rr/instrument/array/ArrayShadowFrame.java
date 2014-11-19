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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rr.org.objectweb.asm.Opcodes;
import rr.org.objectweb.asm.Type;
import rr.org.objectweb.asm.tree.AbstractInsnNode;
import rr.org.objectweb.asm.tree.IincInsnNode;
import rr.org.objectweb.asm.tree.MethodInsnNode;
import rr.org.objectweb.asm.tree.MultiANewArrayInsnNode;
import rr.org.objectweb.asm.tree.VarInsnNode;
import rr.org.objectweb.asm.tree.analysis.AnalyzerException;
import rr.org.objectweb.asm.tree.analysis.Frame;
import rr.org.objectweb.asm.tree.analysis.Interpreter;
import rr.org.objectweb.asm.tree.analysis.Value;

import acme.util.Assert;
import acme.util.Util;


/**
 * A symbolic execution stack frame. A stack frame contains a set of local
 * variable slots, and an operand stack. Warning: long and double values are
 * represented by <i>two</i> slots in local variables, and by <i>one</i> slot
 * in the operand stack.
 * 
 * @author Eric Bruneton
 */
public class ArrayShadowFrame extends Frame {

	protected Set<Integer> arrayShadowInited = new HashSet<Integer>();

	/**
	 * Constructs a new frame with the given size.
	 * 
	 * @param nLocals the maximum number of local variables of the frame.
	 * @param nStack the maximum stack size of the frame.
	 */
	public ArrayShadowFrame(final int nLocals, final int nStack) {
		super(nLocals, nStack+2);
	}

	/**
	 * Constructs a new frame that is identical to the given frame.
	 * 
	 * @param src a frame.
	 */
	public ArrayShadowFrame(final Frame src) {
		super(src);
		this.arrayShadowInited = new HashSet<Integer>(((ArrayShadowFrame)src).arrayShadowInited);
	}

	/**
	 * Copies the state of the given frame into this frame.
	 * 
	 * @param src a frame.
	 * @return this frame.
	 */
	@Override
	public ArrayShadowFrame init(final Frame src) {
//		Util.log(src.values.length + " /// " + values.length);6
		for (int i = 0; i < src.values.length; i++) {
			ArrayShadowValue v = ((ArrayShadowValue)src.values[i]);
			if (v != null) v = v.clone();
			values[i] = v;
		}
		this.top = src.top;
		this.locals = src.locals;
		this.arrayShadowInited = new HashSet<Integer>(((ArrayShadowFrame)src).arrayShadowInited);
		return this;
	}


	public void initArrayShadow(int id) {
		this.arrayShadowInited.add(id);
	}

	public void clearArrayShadow(int id) {
		this.arrayShadowInited.remove(id);
	}

	public boolean getArrayShadow(ArrayShadowValue value) {
		Assert.assertTrue(value.id > -1);
		return this.arrayShadowInited.contains(value.id);
	}

	public Value getFromTop(int down) {
		return this.getStack(this.getStackSize() - 1 - down);
	}

	/**
	 * Merges this frame with the given frame.
	 * 
	 * @param frame a frame.
	 * @param interpreter the interpreter used to merge values.
	 * @return <tt>true</tt> if this frame has been changed as a result of the
	 *         merge operation, or <tt>false</tt> otherwise.
	 * @throws AnalyzerException if the frames have incompatible sizes.
	 */
	@Override
	public boolean merge(final Frame frame, final Interpreter interpreter)
	throws AnalyzerException
	{
		boolean changes = super.merge(frame, interpreter);
		changes = this.arrayShadowInited.retainAll(((ArrayShadowFrame)frame).arrayShadowInited) || changes;

		return changes;
	}

	/**
	 * Merges this frame with the given frame (case of a RET instruction).
	 * 
	 * @param frame a frame
	 * @param access the local variables that have been accessed by the
	 *        subroutine to which the RET instruction corresponds.
	 * @return <tt>true</tt> if this frame has been changed as a result of the
	 *         merge operation, or <tt>false</tt> otherwise.
	 */
	@Override
	public boolean merge(final Frame frame, final boolean[] access) {
		boolean changes = super.merge(frame, access);
		changes = this.arrayShadowInited.retainAll(((ArrayShadowFrame)frame).arrayShadowInited) || changes;

		return changes;
	}

	/**
	 * Returns a string representation of this frame.
	 * 
	 * @return a string representation of this frame.
	 */
	@Override
	public String toString() {
		String s = String.format("%-50s   %s", super.toString(), this.arrayShadowInited);
		return s;
	}

	@Override
	public void execute(
			final AbstractInsnNode insn,
			final Interpreter interpreter) throws AnalyzerException
			{
		Value value1, value2, value3, value4;
		List<Value> values;
		int var;

		switch (insn.getOpcode()) {
			case Opcodes.NOP:
				break;
			case Opcodes.ACONST_NULL:
			case Opcodes.ICONST_M1:
			case Opcodes.ICONST_0:
			case Opcodes.ICONST_1:
			case Opcodes.ICONST_2:
			case Opcodes.ICONST_3:
			case Opcodes.ICONST_4:
			case Opcodes.ICONST_5:
			case Opcodes.LCONST_0:
			case Opcodes.LCONST_1:
			case Opcodes.FCONST_0:
			case Opcodes.FCONST_1:
			case Opcodes.FCONST_2:
			case Opcodes.DCONST_0:
			case Opcodes.DCONST_1:
			case Opcodes.BIPUSH:
			case Opcodes.SIPUSH:
			case Opcodes.LDC:
				push(interpreter.newOperation(insn));
				break;
			case Opcodes.ILOAD:
			case Opcodes.LLOAD:
			case Opcodes.FLOAD:
			case Opcodes.DLOAD:
			case Opcodes.ALOAD: {
				value1 = getLocal(((VarInsnNode) insn).var);

				int id = ((ArrayShadowValue)value1).id;
				if (id > -1) {
					this.initArrayShadow(id);
				}

				push(interpreter.copyOperation(insn, value1));
			}
			break;
			case Opcodes.IALOAD:
			case Opcodes.LALOAD:
			case Opcodes.FALOAD:
			case Opcodes.DALOAD:
			case Opcodes.AALOAD:
			case Opcodes.BALOAD:
			case Opcodes.CALOAD:
			case Opcodes.SALOAD: {
				value2 = pop();
				value1 = pop();

				value3 = interpreter.binaryOperation(insn, value1, value2);

				int id = ((ArrayShadowValue)value3).id;
				if (id > -1) {
					this.initArrayShadow(id);
				}

				push(value3);
			}
			break;
			case Opcodes.ISTORE:
			case Opcodes.LSTORE:
			case Opcodes.FSTORE:
			case Opcodes.DSTORE:
			case Opcodes.ASTORE: {
				value1 = interpreter.copyOperation(insn, pop());
				var = ((VarInsnNode) insn).var;
				
				// Must clear array shadow for dest if it is an array.  Otherwise the cached value will
				// be the one for the old array, not the new one.
				int id2 = ((ArrayShadowValue)this.getLocal(var)).id;
				if (id2 > -1) {
					this.clearArrayShadow(id2);
				}
				
				setLocal(var, value1);

				int id = ((ArrayShadowValue)value1).id;
				if (id > -1) {
					this.initArrayShadow(id);
				}

				if (value1.getSize() == 2) {
					setLocal(var + 1, interpreter.newValue(null));
				}
				if (var > 0) {
					Value local = getLocal(var - 1);
					if (local != null && local.getSize() == 2) {
						setLocal(var + 1, interpreter.newValue(null));
					}
				}
			}
			break;
			case Opcodes.IASTORE:
			case Opcodes.LASTORE:
			case Opcodes.FASTORE:
			case Opcodes.DASTORE:
			case Opcodes.AASTORE:
			case Opcodes.BASTORE:
			case Opcodes.CASTORE:
			case Opcodes.SASTORE: {
				value3 = pop();
				value2 = pop();
				value1 = pop();
				
				//				int id = ((ArrayShadowValue)value1).id;
				//				if (id > -1) {
				//					this.initArrayShadow(id);
				//				}

				interpreter.ternaryOperation(insn, value1, value2, value3);
			}
			break;
			case Opcodes.POP:
				if (pop().getSize() == 2) {
					throw new AnalyzerException(insn, "Illegal use of POP");
				}
				break;
			case Opcodes.POP2:
				if (pop().getSize() == 1) {
					if (pop().getSize() != 1) {
						throw new AnalyzerException(insn, "Illegal use of POP2");
					}
				}
				break;
			case Opcodes.DUP:
				value1 = pop();
				if (value1.getSize() != 1) {
					throw new AnalyzerException(insn, "Illegal use of DUP");
				}
				push(interpreter.copyOperation(insn, value1));
				push(interpreter.copyOperation(insn, value1));
				break;
			case Opcodes.DUP_X1:
				value1 = pop();
				value2 = pop();
				if (value1.getSize() != 1 || value2.getSize() != 1) {
					throw new AnalyzerException(insn, "Illegal use of DUP_X1");
				}
				push(interpreter.copyOperation(insn, value1));
				push(interpreter.copyOperation(insn, value2));
				push(interpreter.copyOperation(insn, value1));
				break;
			case Opcodes.DUP_X2:
				value1 = pop();
				if (value1.getSize() == 1) {
					value2 = pop();
					if (value2.getSize() == 1) {
						value3 = pop();
						if (value3.getSize() == 1) {
							push(interpreter.copyOperation(insn, value1));
							push(interpreter.copyOperation(insn, value3));
							push(interpreter.copyOperation(insn, value2));
							push(interpreter.copyOperation(insn, value1));
							break;
						}
					} else {
						push(interpreter.copyOperation(insn, value1));
						push(interpreter.copyOperation(insn, value2));
						push(interpreter.copyOperation(insn, value1));
						break;
					}
				}
				throw new AnalyzerException(insn, "Illegal use of DUP_X2");
			case Opcodes.DUP2:
				value1 = pop();
				if (value1.getSize() == 1) {
					value2 = pop();
					if (value2.getSize() == 1) {
						push(interpreter.copyOperation(insn, value2));
						push(interpreter.copyOperation(insn, value1));
						push(interpreter.copyOperation(insn, value2));
						push(interpreter.copyOperation(insn, value1));
						break;
					}
				} else {
					push(interpreter.copyOperation(insn, value1));
					push(interpreter.copyOperation(insn, value1));
					break;
				}
				throw new AnalyzerException(insn, "Illegal use of DUP2");
			case Opcodes.DUP2_X1:
				value1 = pop();
				if (value1.getSize() == 1) {
					value2 = pop();
					if (value2.getSize() == 1) {
						value3 = pop();
						if (value3.getSize() == 1) {
							push(interpreter.copyOperation(insn, value2));
							push(interpreter.copyOperation(insn, value1));
							push(interpreter.copyOperation(insn, value3));
							push(interpreter.copyOperation(insn, value2));
							push(interpreter.copyOperation(insn, value1));
							break;
						}
					}
				} else {
					value2 = pop();
					if (value2.getSize() == 1) {
						push(interpreter.copyOperation(insn, value1));
						push(interpreter.copyOperation(insn, value2));
						push(interpreter.copyOperation(insn, value1));
						break;
					}
				}
				throw new AnalyzerException(insn, "Illegal use of DUP2_X1");
			case Opcodes.DUP2_X2:
				value1 = pop();
				if (value1.getSize() == 1) {
					value2 = pop();
					if (value2.getSize() == 1) {
						value3 = pop();
						if (value3.getSize() == 1) {
							value4 = pop();
							if (value4.getSize() == 1) {
								push(interpreter.copyOperation(insn, value2));
								push(interpreter.copyOperation(insn, value1));
								push(interpreter.copyOperation(insn, value4));
								push(interpreter.copyOperation(insn, value3));
								push(interpreter.copyOperation(insn, value2));
								push(interpreter.copyOperation(insn, value1));
								break;
							}
						} else {
							push(interpreter.copyOperation(insn, value2));
							push(interpreter.copyOperation(insn, value1));
							push(interpreter.copyOperation(insn, value3));
							push(interpreter.copyOperation(insn, value2));
							push(interpreter.copyOperation(insn, value1));
							break;
						}
					}
				} else {
					value2 = pop();
					if (value2.getSize() == 1) {
						value3 = pop();
						if (value3.getSize() == 1) {
							push(interpreter.copyOperation(insn, value1));
							push(interpreter.copyOperation(insn, value3));
							push(interpreter.copyOperation(insn, value2));
							push(interpreter.copyOperation(insn, value1));
							break;
						}
					} else {
						push(interpreter.copyOperation(insn, value1));
						push(interpreter.copyOperation(insn, value2));
						push(interpreter.copyOperation(insn, value1));
						break;
					}
				}
				throw new AnalyzerException(insn, "Illegal use of DUP2_X2");
			case Opcodes.SWAP:
				value2 = pop();
				value1 = pop();
				if (value1.getSize() != 1 || value2.getSize() != 1) {
					throw new AnalyzerException(insn, "Illegal use of SWAP");
				}
				push(interpreter.copyOperation(insn, value2));
				push(interpreter.copyOperation(insn, value1));
				break;
			case Opcodes.IADD:
			case Opcodes.LADD:
			case Opcodes.FADD:
			case Opcodes.DADD:
			case Opcodes.ISUB:
			case Opcodes.LSUB:
			case Opcodes.FSUB:
			case Opcodes.DSUB:
			case Opcodes.IMUL:
			case Opcodes.LMUL:
			case Opcodes.FMUL:
			case Opcodes.DMUL:
			case Opcodes.IDIV:
			case Opcodes.LDIV:
			case Opcodes.FDIV:
			case Opcodes.DDIV:
			case Opcodes.IREM:
			case Opcodes.LREM:
			case Opcodes.FREM:
			case Opcodes.DREM:
				value2 = pop();
				value1 = pop();
				push(interpreter.binaryOperation(insn, value1, value2));
				break;
			case Opcodes.INEG:
			case Opcodes.LNEG:
			case Opcodes.FNEG:
			case Opcodes.DNEG:
				push(interpreter.unaryOperation(insn, pop()));
				break;
			case Opcodes.ISHL:
			case Opcodes.LSHL:
			case Opcodes.ISHR:
			case Opcodes.LSHR:
			case Opcodes.IUSHR:
			case Opcodes.LUSHR:
			case Opcodes.IAND:
			case Opcodes.LAND:
			case Opcodes.IOR:
			case Opcodes.LOR:
			case Opcodes.IXOR:
			case Opcodes.LXOR:
				value2 = pop();
				value1 = pop();
				push(interpreter.binaryOperation(insn, value1, value2));
				break;
			case Opcodes.IINC:
				var = ((IincInsnNode) insn).var;
				setLocal(var, interpreter.unaryOperation(insn, getLocal(var)));
				break;
			case Opcodes.I2L:
			case Opcodes.I2F:
			case Opcodes.I2D:
			case Opcodes.L2I:
			case Opcodes.L2F:
			case Opcodes.L2D:
			case Opcodes.F2I:
			case Opcodes.F2L:
			case Opcodes.F2D:
			case Opcodes.D2I:
			case Opcodes.D2L:
			case Opcodes.D2F:
			case Opcodes.I2B:
			case Opcodes.I2C:
			case Opcodes.I2S:
				push(interpreter.unaryOperation(insn, pop()));
				break;
			case Opcodes.LCMP:
			case Opcodes.FCMPL:
			case Opcodes.FCMPG:
			case Opcodes.DCMPL:
			case Opcodes.DCMPG:
				value2 = pop();
				value1 = pop();
				push(interpreter.binaryOperation(insn, value1, value2));
				break;
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
			case Opcodes.IFLT:
			case Opcodes.IFGE:
			case Opcodes.IFGT:
			case Opcodes.IFLE:
				interpreter.unaryOperation(insn, pop());
				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
			case Opcodes.IF_ACMPEQ:
			case Opcodes.IF_ACMPNE:
				value2 = pop();
				value1 = pop();
				interpreter.binaryOperation(insn, value1, value2);
				break;
			case Opcodes.GOTO:
				break;
			case Opcodes.JSR:
				push(interpreter.newOperation(insn));
				break;
			case Opcodes.RET:
				break;
			case Opcodes.TABLESWITCH:
			case Opcodes.LOOKUPSWITCH:
			case Opcodes.IRETURN:
			case Opcodes.LRETURN:
			case Opcodes.FRETURN:
			case Opcodes.DRETURN:
			case Opcodes.ARETURN:
				interpreter.unaryOperation(insn, pop());
				break;
			case Opcodes.RETURN:
				break;
			case Opcodes.GETSTATIC:
				push(interpreter.newOperation(insn));
				break;
			case Opcodes.PUTSTATIC:
				interpreter.unaryOperation(insn, pop());
				break;
			case Opcodes.GETFIELD:
				//Util.logf(this + "  ->  ");
				push(interpreter.unaryOperation(insn, pop()));
				//Util.logf(this + "  .   ");
				break;
			case Opcodes.PUTFIELD:
				value2 = pop();
				value1 = pop();
				interpreter.binaryOperation(insn, value1, value2);
				break;
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				values = new ArrayList<Value>();
				String desc = ((MethodInsnNode) insn).desc;
				for (int i = Type.getArgumentTypes(desc).length; i > 0; --i) {
					values.add(0, pop());
				}
				if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
					values.add(0, pop());
				}
				if (Type.getReturnType(desc) == Type.VOID_TYPE) {
					interpreter.naryOperation(insn, values);
				} else {
					push(interpreter.naryOperation(insn, values));
				}
				break;
			case Opcodes.NEW:
				push(interpreter.newOperation(insn));
				break;
			case Opcodes.NEWARRAY:
			case Opcodes.ANEWARRAY:
			case Opcodes.ARRAYLENGTH:
				push(interpreter.unaryOperation(insn, pop()));
				break;
			case Opcodes.ATHROW:
				interpreter.unaryOperation(insn, pop());
				break;
			case Opcodes.CHECKCAST:
			case Opcodes.INSTANCEOF:
				push(interpreter.unaryOperation(insn, pop()));
				break;
			case Opcodes.MONITORENTER:
			case Opcodes.MONITOREXIT:
				interpreter.unaryOperation(insn, pop());
				break;
			case Opcodes.MULTIANEWARRAY:
				values = new ArrayList();
				for (int i = ((MultiANewArrayInsnNode) insn).dims; i > 0; --i) {
					values.add(0, pop());
				}
				push(interpreter.naryOperation(insn, values));
				break;
			case Opcodes.IFNULL:
			case Opcodes.IFNONNULL:
				interpreter.unaryOperation(insn, pop());
				break;
			default:
				throw new RuntimeException("Illegal opcode");
		}
			}


}
