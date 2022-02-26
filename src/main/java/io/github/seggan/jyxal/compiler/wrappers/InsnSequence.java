package io.github.seggan.jyxal.compiler.wrappers;

import org.objectweb.asm.tree.AbstractInsnNode;

import java.util.ArrayList;
import java.util.List;

record InsnSequence(int... insns) {

    public List<AbstractInsnNode> matches(AbstractInsnNode insn) {
        List<AbstractInsnNode> matches = new ArrayList<>();
        for (int i : insns) {
            if (insn.getOpcode() != i) {
                return new ArrayList<>();
            }
            matches.add(insn);
            insn = insn.getNext();
        }

        return matches;
    }
}
