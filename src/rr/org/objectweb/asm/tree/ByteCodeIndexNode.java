package rr.org.objectweb.asm.tree;

import java.util.Map;

import rr.org.objectweb.asm.MethodVisitor;

public class ByteCodeIndexNode extends AbstractInsnNode {

    public int index;

    public ByteCodeIndexNode(final int index) {
        super(-1);
        this.index = index;
    }

    public int getType() {
        return BYTE_CODE_INDEX;
    }

    public void accept(final MethodVisitor mv) {
        mv.visitByteCodeIndex(index);
    }

    public AbstractInsnNode clone(final Map<LabelNode, LabelNode> labels) {
        return new ByteCodeIndexNode(index);
    }
    
    public String toString() {
    	return "BCI:" + index;
    }
}
