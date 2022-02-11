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

    public static void addBigComplex(String number, MethodVisitorWrapper mv) {
        AsmHelper.addBigDecimal(number, mv);
        mv.visitMethodInsn(
            INVOKESTATIC,
            "runtime/math/BigComplex",
            "valueOf",
            "(Ljava/math/BigDecimal;)Lruntime/math/BigComplex;",
            false
        );
        mv.visitIincInsn(mv.getStackVar(), 1);
    }

    public static void pushOne(MethodVisitorWrapper mv) {
        mv.visitIincInsn(mv.getStackVar(), 1);
    }

    public static void popOne(MethodVisitorWrapper mv) {
        mv.visitIincInsn(mv.getStackVar(), -1);
    }

    public static void selectNumberInsn(MethodVisitorWrapper mv, int number) {
        switch (number) {
            case -1 -> mv.visitInsn(ICONST_M1);
            case 0 -> mv.visitInsn(ICONST_0);
            case 1 -> mv.visitInsn(ICONST_1);
            case 2 -> mv.visitInsn(ICONST_2);
            case 3 -> mv.visitInsn(ICONST_3);
            case 4 -> mv.visitInsn(ICONST_4);
            case 5 -> mv.visitInsn(ICONST_5);
            default -> mv.visitLdcInsn(number);
        }
    }
}
