package io.github.seggan.jyxal.compiler.symjy

import io.github.seggan.jyxal.antlr.SymJyBaseVisitor
import io.github.seggan.jyxal.antlr.SymJyLexer
import io.github.seggan.jyxal.antlr.SymJyParser
import io.github.seggan.jyxal.compiler.JyxalCompileException
import io.github.seggan.jyxal.runtime.times
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Token
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import kotlin.math.absoluteValue

class SymJyCompiler private constructor(private val mv: MathMethod) : SymJyBaseVisitor<Unit>() {

    override fun visitNumber(ctx: SymJyParser.NumberContext) {
        mv.visitNumber(ctx.text)
    }

    override fun visitVariable(ctx: SymJyParser.VariableContext) {
        mv.loadVar(ctx.text[0])
    }

    override fun visitPowExpression(ctx: SymJyParser.PowExpressionContext) {
        visit(ctx.atom())
        val expr = ctx.expression()
        if (expr != null) {
            visit(expr)
            mv.powNumbers()
        }
    }

    override fun visitMultiplyingExpression(ctx: SymJyParser.MultiplyingExpressionContext) {
        visit(ctx.powExpression(0))
        if (ctx.powExpression().size > 1) {
            for (i in 1 until ctx.powExpression().size) {
                visit(ctx.powExpression(i))
                val op = ctx.MUL_OPERATOR(i - 1)?.text
                if (op == null || op == "*") {
                    mv.multiplyNumbers()
                } else {
                    mv.divideNumbers()
                }
            }
        }
    }

    override fun visitExpression(ctx: SymJyParser.ExpressionContext) {
        visit(ctx.multiplyingExpression(0))
        if (ctx.multiplyingExpression().size > 1) {
            for (i in 1 until ctx.multiplyingExpression().size) {
                visit(ctx.multiplyingExpression(i))
                if (ctx.ADD_OPERATOR(i - 1).text == "+") {
                    mv.addNumbers()
                } else {
                    mv.subtractNumbers()
                }
            }
        }
    }

    override fun visitFunctionCall(ctx: SymJyParser.FunctionCallContext) {
        visit(ctx.multiplyingExpression())
        when (val function = ctx.FUNCTION_NAME().text) {
            "s" -> mv.bigMathStuff("sin", "complex|context")
            "c" -> mv.bigMathStuff("cos", "complex|context")
            "t" -> mv.bigMathStuff("tan", "complex|context")
            "as" -> mv.bigMathStuff("asin", "complex|context")
            "ac" -> mv.bigMathStuff("acos", "complex|context")
            "at" -> mv.bigMathStuff("atan", "complex|context")
            "l" -> {
                mv.visitNumber("10")
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "io/github/seggan/jyxal/runtime/math/RuntimeMethods",
                        "loga",
                        "(Lio/github/seggan/jyxal/runtime/math/BigComplex;Lio/github/seggan/jyxal/runtime/math/BigComplex;)Lio/github/seggan/jyxal/runtime/math/BigComplex;",
                        false
                )
            }
            "L" -> mv.bigMathStuff("log", "complex|context")
            "S" -> mv.powWith("2")
            "C" -> mv.powWith("3")
            "h" -> mv.divideBy("2")
            "D" -> mv.multiplyBy("2")
            "R" -> mv.bigMathStuff("sqrt", "complex|context")
            "\\" -> {
                mv.visitNumber("1")
                mv.visitInsn(Opcodes.SWAP)
                mv.divideNumbers()
            }
            "!" -> mv.bigMathStuff("factorial", "complex|context")
            else -> throw JyxalCompileException("Unknown function: $function")
        }
    }

    companion object {

        private var methodCounter = 0

        /**
         * Does a bit of preprocessing on the input string so that the parser can handle it
         */
        fun transpile(input: String): String = buildString {
            var parenCount = 0
            for (c in input) {
                if (c == '(') {
                    parenCount++
                } else if (c == ')') {
                    parenCount--
                }
                when (c) {
                    '}' -> {
                        append("))")
                        parenCount -= 2
                    }
                    '[' -> {
                        append(")))")
                        parenCount -= 3
                    }
                    ']' -> {
                        append(")" * parenCount)
                        parenCount = 0
                    }
                    else -> append(c)
                }
            }
            if (parenCount > 0) {
                append(")" * parenCount)
            } else if (parenCount < 0) {
                insert(0, "(" * parenCount.absoluteValue)
            }
        }

        fun compile(input: String, cw: ClassWriter): Triple<String, Int, String> {
            val lexer = SymJyLexer(CharStreams.fromString(input))
            val vars = lexer.allTokens.filter { it.type == SymJyLexer.VARIABLE }
                    .map(Token::getText).toSet()
            val arity = if (vars.contains("z")) 3 else if (vars.contains("y")) 2 else 1
            val parser = SymJyParser(CommonTokenStream(SymJyLexer(CharStreams.fromString(input))))
            val signature = "(${"Lio/github/seggan/jyxal/runtime/math/BigComplex;" * arity})Lio/github/seggan/jyxal/runtime/math/BigComplex;"
            val name = "nativeMathMethod$${methodCounter++}"
            val method = cw.visitMethod(
                    Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                    name,
                    signature,
                    null,
                    null
            )
            parser.file().accept(SymJyCompiler(MathMethod(method, arity)))
            method.visitInsn(Opcodes.ARETURN)
            method.visitMaxs(0, 0)
            method.visitEnd()
            return Triple(name, arity, signature)
        }
    }
}