package io.github.seggan.jyxal.compiler;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MethodVisitorWrapper extends MethodVisitor implements Opcodes {

    private final int stackVar;
    private final int ctxVar;

    public MethodVisitorWrapper(MethodVisitor methodVisitor, int stackVar, int ctxVar) {
        super(Opcodes.ASM7, methodVisitor);
        this.stackVar = stackVar;
        this.ctxVar = ctxVar;

        visitInsn(ICONST_0);
        visitVarInsn(ISTORE, stackVar);

        AsmHelper.addBigComplex("0", this);
        visitVarInsn(ASTORE, ctxVar);
    }

    public int getStackVar() {
        return stackVar;
    }

    public int getCtxVar() {
        return ctxVar;
    }
}
