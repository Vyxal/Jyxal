package io.github.seggan.jyxal.compiler.optimization;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.ListIterator;
import java.util.Set;

public class PushPopOptimizer extends MethodNode implements Opcodes {

    public PushPopOptimizer(int access, String name, String desc, String signature, String[] exceptions, MethodVisitor mv) {
        super(ASM9, access, name, desc, signature, exceptions);
        this.mv = mv;
    }

    @Override
    public void visitEnd() {
        ListIterator<AbstractInsnNode> it = instructions.iterator();
        Set<AbstractInsnNode> toRemove = new HashSet<>();
        while (it.hasNext()) {
            AbstractInsnNode insn = it.next();
            if (insn.getOpcode() == INVOKEVIRTUAL
                && insn instanceof MethodInsnNode methodInsnNode
                && methodInsnNode.name.equals("push")
                && methodInsnNode.desc.equals("(Ljava/lang/Object;)V")
                && methodInsnNode.owner.equals("runtime/ProgramStack")) {
                AbstractInsnNode next = insn.getNext();
                if (next.getOpcode() == ALOAD) {
                    AbstractInsnNode next1 = next.getNext();
                    if (next1.getOpcode() == INVOKEVIRTUAL
                        && next1 instanceof MethodInsnNode methodInsnNode2
                        && methodInsnNode2.name.equals("pop")
                        && methodInsnNode2.desc.equals("()Ljava/lang/Object;")
                        && methodInsnNode2.owner.equals("runtime/ProgramStack")) {
                        toRemove.add(insn);
                        toRemove.add(next);
                        toRemove.add(next1);
                    }
                }
            }
        }

        for (AbstractInsnNode insn : toRemove) {
            instructions.remove(insn);
        }

        accept(mv);
    }
}
