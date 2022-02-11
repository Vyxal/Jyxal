package io.github.seggan.jyxal.compiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodVisitorWrapper extends MethodVisitor implements Opcodes {

    private final int stackVar;
    private final int ctxVar;

    public MethodVisitorWrapper(ClassWriter cw, int access, String name, String desc) {
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
}
