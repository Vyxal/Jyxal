package io.github.seggan.jyxal.compiler.wrappers;

import io.github.seggan.jyxal.compiler.AsmHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class JyxalMethod extends MethodNode implements Opcodes {

    private final int stackVar;
    private final int ctxVar;

    private final Set<ContextualVariable> reservedVars = new HashSet<>();

    JyxalMethod(ClassWriter cw, int access, String name, String desc) {
        super(Opcodes.ASM7, access, name, desc, null, null);
        this.mv = cw.visitMethod(access, name, desc, null, null);
        this.stackVar = Type.getArgumentTypes(desc).length;
        this.ctxVar = stackVar + 1;

        visitTypeInsn(NEW, "runtime/ProgramStack");
        visitInsn(DUP);
        visitMethodInsn(INVOKESPECIAL, "runtime/ProgramStack", "<init>", "()V", false);
        visitVarInsn(ASTORE, stackVar);

        AsmHelper.addBigComplex("0", this);
        visitVarInsn(ASTORE, ctxVar);
    }

    public int getStackVar() {
        return stackVar;
    }

    public int getCtxVar() {
        return ctxVar;
    }

    public void loadStack() {
        visitVarInsn(ALOAD, stackVar);
    }

    public void loadContextVar() {
        visitVarInsn(ALOAD, ctxVar);
    }

    public ContextualVariable reserveVar() {
        int max = 0;
        for (ContextualVariable var : reservedVars) {
            if (var.index > max) {
                max = var.index;
            }
        }
        if (max == 0) {
            ContextualVariable var = new ContextualVariable(ctxVar + 1, this);
            reservedVars.add(var);
            return var;
        } else {
            for (int i = ctxVar + 1; i < max; i++) {
                ContextualVariable var = new ContextualVariable(i, this);
                if (!reservedVars.contains(var)) {
                    reservedVars.add(var);
                    return var;
                }
            }
            ContextualVariable var = new ContextualVariable(ctxVar + 1, this);
            reservedVars.add(var);
            return var;
        }
    }

    void freeVar(ContextualVariable var) {
        reservedVars.remove(var);
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

    private static record InsnSequence(int... insns) {

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
}
