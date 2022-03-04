package io.github.seggan.jyxal.compiler.wrappers;

import io.github.seggan.jyxal.CompilerOptions;
import io.github.seggan.jyxal.compiler.AsmHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
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
    private boolean optimise = !CompilerOptions.OPTIONS.contains(CompilerOptions.DONT_OPTIMISE);

    JyxalMethod(ClassWriter cw, int access, String name, String desc) {
        super(Opcodes.ASM7, access, name, desc, null, null);
        this.mv = cw.visitMethod(access, name, desc, null, null);

        int stackVar1;

        // lambdas have the program stack passed as the only argument
        if (name.startsWith("lambda$")) {
            stackVar1 = 0;
        } else {
            stackVar1 = Type.getArgumentTypes(desc).length;

            visitTypeInsn(NEW, "runtime/ProgramStack");
            visitInsn(DUP);
            visitMethodInsn(INVOKESPECIAL, "runtime/ProgramStack", "<init>", "()V", false);
            visitVarInsn(ASTORE, stackVar1);
        }

        this.stackVar = stackVar1;
        this.ctxVar = stackVar + 1;

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
        ContextualVariable var = new ContextualVariable(max == 0 ? ctxVar + 1 : max + 1, this);
        reservedVars.add(var);
        return var;
    }

    void freeVar(ContextualVariable var) {
        reservedVars.remove(var);
    }

    public void setOptimise(boolean optimise) {
        this.optimise = optimise;
    }

    @Override
    public void visitEnd() {
        if (optimise) {
            List<InsnList> codeBlocks = new ArrayList<>();
            InsnList code = new InsnList();
            for (ListIterator<AbstractInsnNode> iterator = instructions.iterator(); iterator.hasNext(); ) {
                AbstractInsnNode insn = iterator.next();
                iterator.remove();
                code.add(insn);
                if (insn instanceof LabelNode || insn.getOpcode() == RETURN || insn.getOpcode() == ATHROW
                        || insn.getOpcode() == IRETURN || insn.getOpcode() == LRETURN
                        || insn.getOpcode() == FRETURN || insn.getOpcode() == DRETURN
                        || insn.getOpcode() == ARETURN) {
                    codeBlocks.add(code);
                    code = new InsnList();
                }
            }

            for (InsnList block : codeBlocks) {
                Optimiser.optimise(block, this);
            }

            for (InsnList block : codeBlocks) {
                instructions.add(block);
            }

            // The instruction sequence for the context var
            InsnSequence contextInit = new InsnSequence(NEW, DUP, LDC, INVOKESPECIAL, INVOKESTATIC, ASTORE);
            List<AbstractInsnNode> contextVarInit = null;
            boolean contextVarUsed = false;

            ListIterator<AbstractInsnNode> it = instructions.iterator();
            while (it.hasNext()) {
                AbstractInsnNode insn = it.next();
                if (contextVarInit == null) {
                    List<AbstractInsnNode> matches = contextInit.matches(insn);
                    if (!matches.isEmpty()
                            && matches.get(matches.size() - 1) instanceof VarInsnNode varInsnNode
                            && varInsnNode.var == ctxVar) {
                        contextVarInit = matches;
                        int size = contextVarInit.size() - 1;
                        for (int i = 0; i < size; i++) {
                            it.next();
                        }
                    }
                }

                if (insn instanceof VarInsnNode varInsnNode && varInsnNode.var == ctxVar) {
                    contextVarUsed = true;
                }
            }

            if (contextVarInit != null && !contextVarUsed) {
                for (AbstractInsnNode insn : contextVarInit) {
                    instructions.remove(insn);
                }
            }
        }

        accept(mv);
    }
}
