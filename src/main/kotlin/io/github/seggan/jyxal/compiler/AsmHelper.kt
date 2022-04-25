package io.github.seggan.jyxal.compiler

import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

object AsmHelper : Opcodes {
    fun addBigDecimal(number: String?, mv: MethodVisitor) {
        mv.visitTypeInsn(Opcodes.NEW, "java/math/BigDecimal")
        mv.visitInsn(Opcodes.DUP)
        mv.visitLdcInsn(number)
        mv.visitMethodInsn(
            Opcodes.INVOKESPECIAL,
            "java/math/BigDecimal",
            "<init>",
            "(Ljava/lang/String;)V",
            false
        )
    }

    fun addBigComplex(number: String?, mv: MethodVisitor) {
        addBigDecimal(number, mv)
        mv.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "runtime/math/BigComplex",
            "valueOf",
            "(Ljava/math/BigDecimal;)Lruntime/math/BigComplex;",
            false
        )
    }

    fun push(mv: JyxalMethod) {
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "runtime/ProgramStack",
            "push",
            "(Ljava/lang/Object;)V",
            false
        )
    }

    fun pop(mv: JyxalMethod) {
        mv.loadStack()
        mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            "runtime/ProgramStack",
            "pop",
            "()Ljava/lang/Object;",
            false
        )
    }

    fun selectNumberInsn(mv: MethodVisitor, number: Int) {
        when (number) {
            -1 -> mv.visitInsn(Opcodes.ICONST_M1)
            0 -> mv.visitInsn(Opcodes.ICONST_0)
            1 -> mv.visitInsn(Opcodes.ICONST_1)
            2 -> mv.visitInsn(Opcodes.ICONST_2)
            3 -> mv.visitInsn(Opcodes.ICONST_3)
            4 -> mv.visitInsn(Opcodes.ICONST_4)
            5 -> mv.visitInsn(Opcodes.ICONST_5)
            else -> {
                if (number >= Byte.MIN_VALUE && number <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.BIPUSH, number)
                } else if (number >= Short.MIN_VALUE && number <= Short.MAX_VALUE) {
                    mv.visitIntInsn(Opcodes.SIPUSH, number)
                } else {
                    mv.visitLdcInsn(number)
                }
            }
        }
    }
}