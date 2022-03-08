package io.github.seggan.jyxal.compiler.wrappers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.HashSet;
import java.util.Set;

public class Optimiser implements Opcodes {

    public static void optimise(InsnList codeBlock, JyxalMethod jyxalMethod) {
        // first remove all stack loads
        Set<AbstractInsnNode> toRemove = new HashSet<>();
        for (AbstractInsnNode insn : codeBlock) {
            if (insn instanceof VarInsnNode varInsnNode && varInsnNode.var == jyxalMethod.getStackVar()
                    && varInsnNode.getOpcode() == ALOAD) {
                toRemove.add(insn);
                AbstractInsnNode next = insn.getNext();
                if (next != null && next.getOpcode() == SWAP) {
                    toRemove.add(next);
                }
            }
        }
        for (AbstractInsnNode insn : toRemove) {
            codeBlock.remove(insn);
        }
        toRemove.clear();
        // then remove all push-pop pairs
        for (AbstractInsnNode insn : codeBlock) {
            if (insn.getOpcode() == INVOKEVIRTUAL
                    && insn instanceof MethodInsnNode methodInsnNode
                    && methodInsnNode.name.equals("push")
                    && methodInsnNode.desc.equals("(Ljava/lang/Object;)V")
                    && methodInsnNode.owner.equals("runtime/ProgramStack")) {
                AbstractInsnNode next = insn.getNext();
                if (next.getOpcode() == INVOKEVIRTUAL
                        && next instanceof MethodInsnNode methodInsnNode2
                        && methodInsnNode2.name.equals("pop")
                        && methodInsnNode2.desc.equals("()Ljava/lang/Object;")
                        && methodInsnNode2.owner.equals("runtime/ProgramStack")) {
                    toRemove.add(insn);
                    toRemove.add(next);
                }
            }
        }
        for (AbstractInsnNode insn : toRemove) {
            codeBlock.remove(insn);
        }
        // now we correct the load operations
        for (AbstractInsnNode insn : codeBlock) {
            if (insn instanceof MethodInsnNode methodInsnNode) {
                if (methodInsnNode.owner.equals("runtime/ProgramStack") && !methodInsnNode.name.equals("<init>")) {
                    int arguments = (Type.getArgumentsAndReturnSizes(methodInsnNode.desc) >> 2) - 1;
                    codeBlock.insertBefore(insn, new VarInsnNode(ALOAD, jyxalMethod.getStackVar()));
                    if (arguments != 0) {
                        // one argument
                        codeBlock.insertBefore(insn, new InsnNode(SWAP));
                    }
                } else if (methodInsnNode.owner.equals("runtime/RuntimeMethods")
                        && methodInsnNode.desc.startsWith("(Lruntime/ProgramStack;)")) {
                    // these all take the stack as input
                    codeBlock.insertBefore(insn, new VarInsnNode(ALOAD, jyxalMethod.getStackVar()));
                }
            }
        }
    }
}
