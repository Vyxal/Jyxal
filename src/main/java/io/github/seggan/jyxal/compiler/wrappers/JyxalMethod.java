package io.github.seggan.jyxal.compiler.wrappers;

import io.github.seggan.jyxal.compiler.AsmHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JyxalMethod extends MethodVisitor implements Opcodes {

    private final int stackVar;
    private final int ctxVar;

    private final Set<ContextualVariable> reservedVars = new HashSet<>();

    public JyxalMethod(ClassWriter cw, int access, String name, String desc) {
        super(Opcodes.ASM7, cw.visitMethod(access, name, desc, null, null));
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
}
