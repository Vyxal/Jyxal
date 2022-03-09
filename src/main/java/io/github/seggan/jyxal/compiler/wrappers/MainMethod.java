package io.github.seggan.jyxal.compiler.wrappers;

import org.objectweb.asm.ClassWriter;

public class MainMethod extends JyxalMethod {

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
    }
}
