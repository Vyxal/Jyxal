package io.github.seggan.jyxal.compiler.symjy

import io.github.seggan.jyxal.compiler.AsmHelper
import io.github.seggan.jyxal.compiler.JyxalCompileException
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * A utility class for compilation of mathematical expressions
 */
internal class MathMethod internal constructor(mv: MethodVisitor, arity: Int) : MethodVisitor(Opcodes.ASM9, mv) {

    init {
        mv.visitCode()
    }

    fun visitNumber(num: String) {
        if (num.length > 18 || num.contains('.')) {
            AsmHelper.addBigComplex(num, mv)
        } else {
            val num1 = num.toLong()
            if (num1 in Int.MIN_VALUE..Int.MAX_VALUE) {
                AsmHelper.selectNumberInsn(mv, num1.toInt())
                mv.visitInsn(Opcodes.I2L)
            } else {
                visitLdcInsn(num1)
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "io/github/seggan/jyxal/runtime/math/BigComplex",
                    "valueOf",
                    "(J)Lio/github/seggan/jyxal/runtime/math/BigComplex;",
                    false
            )
        }
    }

    fun addNumbers() {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "io/github/seggan/jyxal/runtime/math/BigComplex",
                "add",
                "(Lio/github/seggan/jyxal/runtime/math/BigComplex;)Lio/github/seggan/jyxal/runtime/math/BigComplex;",
                false
        )
    }

    fun addWith(num: String) {
        visitNumber(num)
        addNumbers()
    }

    fun subtractNumbers() {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "io/github/seggan/jyxal/runtime/math/BigComplex",
                "subtract",
                "(Lio/github/seggan/jyxal/runtime/math/BigComplex;)Lio/github/seggan/jyxal/runtime/math/BigComplex;",
                false
        )
    }

    fun subtractWith(num: String) {
        visitNumber(num)
        subtractNumbers()
    }

    fun multiplyNumbers() {
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "io/github/seggan/jyxal/runtime/math/BigComplex",
                "multiply",
                "(Lio/github/seggan/jyxal/runtime/math/BigComplex;)Lio/github/seggan/jyxal/runtime/math/BigComplex;",
                false
        )
    }

    fun multiplyBy(num: String) {
        visitNumber(num)
        multiplyNumbers()
    }

    fun divideNumbers() {
        loadMathContext()
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "io/github/seggan/jyxal/runtime/math/BigComplex",
                "divide",
                "(Lio/github/seggan/jyxal/runtime/math/BigComplex;Ljava/math/MathContext;)Lio/github/seggan/jyxal/runtime/math/BigComplex;",
                false
        )
    }

    fun divideBy(num: String) {
        visitNumber(num)
        divideNumbers()
    }

    fun powNumbers() {
        loadMathContext()
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "io/github/seggan/jyxal/runtime/math/BigComplexMath",
                "pow",
                "(Lio/github/seggan/jyxal/runtime/math/BigComplex;Lio/github/seggan/jyxal/runtime/math/BigComplex;Ljava/math/MathContext;)Lio/github/seggan/jyxal/runtime/math/BigComplex;",
                false
        )
    }

    fun powWith(num: String) {
        visitNumber(num)
        powNumbers()
    }

    fun loadVar(variable: Char) {
        when (variable) {
            'x' -> mv.visitVarInsn(Opcodes.ALOAD, 0)
            'y' -> mv.visitVarInsn(Opcodes.ALOAD, 1)
            'z' -> mv.visitVarInsn(Opcodes.ALOAD, 2)
            else -> throw JyxalCompileException("Unknown variable: $variable")
        }
    }

    fun loadMathContext() {
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "java/math/MathContext",
                "DECIMAL128",
                "Ljava/math/MathContext;"
        )
    }

    fun bigMathStuff(name: String, args: String) {
        val desc = buildString {
            append('(')
            for (arg in args.split('|')) {
                when (arg.lowercase()) {
                    "complex" -> append("Lio/github/seggan/jyxal/runtime/math/BigComplex;")
                    "context" -> append("Ljava/math/MathContext;")
                    else -> throw JyxalCompileException("Unknown argument: $arg")
                }
            }
            append(")Lio/github/seggan/jyxal/runtime/math/BigComplex;")
        }
        if (args.endsWith("context")) {
            loadMathContext()
        }
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "io/github/seggan/jyxal/runtime/math/BigComplexMath",
                name,
                desc,
                false
        )
    }
}