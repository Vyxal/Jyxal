package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.CompilerOptions;
import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod;
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

    public static void addBigComplex(String number, MethodVisitor mv) {
        AsmHelper.addBigDecimal(number, mv);
        mv.visitMethodInsn(
            INVOKESTATIC,
            "runtime/math/BigComplex",
            "valueOf",
            "(Ljava/math/BigDecimal;)Lruntime/math/BigComplex;",
            false
        );
    }

    public static void push(JyxalMethod mv) {
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "runtime/ProgramStack",
            "push",
            "(Ljava/lang/Object;)V",
            false
        );
    }

    public static void pop(JyxalMethod mv) {
        mv.loadStack();
        mv.visitMethodInsn(
            INVOKEVIRTUAL,
            "runtime/ProgramStack",
            "pop",
            "()Ljava/lang/Object;",
            false
        );
    }

    public static void selectNumberInsn(JyxalMethod mv, int number) {
        switch (number) {
            case -1 -> mv.visitInsn(ICONST_M1);
            case 0 -> mv.visitInsn(ICONST_0);
            case 1 -> mv.visitInsn(ICONST_1);
            case 2 -> mv.visitInsn(ICONST_2);
            case 3 -> mv.visitInsn(ICONST_3);
            case 4 -> mv.visitInsn(ICONST_4);
            case 5 -> mv.visitInsn(ICONST_5);
            default -> {
                if (number >= Byte.MIN_VALUE && number <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(BIPUSH, number);
                } else if (number >= Short.MIN_VALUE && number <= Short.MAX_VALUE) {
                    mv.visitIntInsn(SIPUSH, number);
                } else {
                    mv.visitLdcInsn(number);
                }
            }
        }
    }
}
