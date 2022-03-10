package io.github.seggan.jyxal.compiler.wrappers;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

public class MainMethod extends JyxalMethod {

    private final Label start = new Label();
    private final Label end = new Label();

    MainMethod(ClassWriter cw, int access, String name, String desc) {
        super(cw, access, name, desc);

        stackVar = 1;
        ctxVar = 2;

        visitTypeInsn(NEW, "runtime/ProgramStack");
        visitInsn(DUP);
        visitVarInsn(ALOAD, 0);
        visitMethodInsn(INVOKESPECIAL, "runtime/ProgramStack", "<init>", "([Ljava/lang/String;)V", false);
        visitVarInsn(ASTORE, stackVar);

        mv.visitFieldInsn(
                GETSTATIC,
                "runtime/math/BigComplex",
                "ZERO",
                "Lruntime/math/BigComplex;"
        );
        visitVarInsn(ASTORE, ctxVar);

        visitLabel(start);
        visitLocalVariable("stack", "Lruntime/ProgramStack;", null, start, end, stackVar);

        visitLocalVariable("ctx", "Ljava/lang/Object;", null, start, end, ctxVar);
    }

    @Override
    public void visitEnd() {
        visitLabel(end);
        super.visitEnd();
    }
}
