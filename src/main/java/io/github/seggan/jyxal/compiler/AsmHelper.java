package io.github.seggan.jyxal.compiler;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public final class AsmHelper implements Opcodes {

    private AsmHelper() {
    }

    public static void addBigDecimal(String number, MethodVisitor mv) {
        mv.visitTypeInsn(NEW, "java/math/BigDecimal");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(number);
        mv.visitMethodInsn(
            INVOKESPECIAL,
            "java/math/BigDecimal",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        );
    }
}
