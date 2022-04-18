package io.github.seggan.jyxal.compiler

import io.github.seggan.jyxal.CompilerOptions
import io.github.seggan.jyxal.antlr.VyxalParser
import io.github.seggan.jyxal.antlr.VyxalParser.*
import io.github.seggan.jyxal.antlr.VyxalParserBaseVisitor
import io.github.seggan.jyxal.compiler.wrappers.JyxalClassWriter
import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod
import io.github.seggan.jyxal.runtime.text.Compression.decompress
import io.github.seggan.jyxal.runtime.unescapeString
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.util.CheckClassAdapter
import java.io.FileOutputStream
import java.io.PrintWriter
import java.util.ArrayDeque
import java.util.Deque
import java.util.function.Consumer
import java.util.regex.Pattern

class Compiler private constructor(private val classWriter: JyxalClassWriter, private val clinit: MethodVisitor) : VyxalParserBaseVisitor<Unit>(), Opcodes {

    private val variables: MutableSet<String> = HashSet()
    private val contextVariables: MutableSet<String> = HashSet()

    private val callStack: Deque<JyxalMethod> = ArrayDeque()
    private val loopStack: Deque<Loop> = ArrayDeque()

    private val aliases: MutableMap<String, ProgramContext> = HashMap()

    private var listCounter = 0
    private var lambdaCounter = 0

    override fun visitInteger(ctx: IntegerContext) {
        val mv = callStack.peek()
        mv.loadStack()
        AsmHelper.addBigComplex(ctx.text, mv)
        AsmHelper.push(mv)
    }

    override fun visitComplex_number(ctx: Complex_numberContext) {
        val mv = callStack.peek()
        val parts = COMPLEX_SEPARATOR.split(ctx.text)
        mv.loadStack()
        AsmHelper.addBigDecimal(parts[0], mv)
        AsmHelper.addBigDecimal(parts[1], mv)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(Ljava/math/BigDecimal;Ljava/math/BigDecimal;)Lruntime/math/BigComplex;",
                false
        )
        AsmHelper.push(mv)
    }

    override fun visitNormal_string(ctx: Normal_stringContext) {
        val mv = callStack.peek()
        val text = ctx.text
        mv.loadStack()
        mv.visitLdcInsn(decompress(unescapeString(text.substring(1, text.length - 1))))
        AsmHelper.push(mv)
    }

    override fun visitSingle_char_string(ctx: Single_char_stringContext) {
        val mv = callStack.peek()
        mv.loadStack()
        mv.visitLdcInsn(decompress(unescapeString(ctx.text.substring(1))))
        AsmHelper.push(mv)
    }

    override fun visitDouble_char_string(ctx: Double_char_stringContext) {
        val mv = callStack.peek()
        mv.loadStack()
        mv.visitLdcInsn(decompress(unescapeString(ctx.text.substring(1))))
        AsmHelper.push(mv)
    }

    override fun visitList(ctx: ListContext) {
        val method = callStack.peek()
        method.loadStack()
        AsmHelper.selectNumberInsn(method, ctx.program().size)
        method.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object")
        val program = ctx.program()
        for (i in program.indices) {
            val item = program[i]
            method.visitInsn(Opcodes.DUP)
            AsmHelper.selectNumberInsn(method, i)
            if (item.childCount == 1 && item.getChild(0) is LiteralContext) {
                // we can inline the literal
                visit(item.getChild(0))
                // we need to pop the literal value, it's going to get optimized away anyway
                AsmHelper.pop(method)
                method.visitInsn(Opcodes.SWAP)
                method.visitInsn(Opcodes.POP)
            } else {
                val methodName = "listInit$" + listCounter++
                val mv = classWriter.visitMethod(
                        Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                        methodName,
                        "()Ljava/lang/Object;"
                )
                mv.visitCode()
                callStack.push(mv)
                visit(item)
                callStack.pop()
                AsmHelper.pop(mv)
                mv.visitInsn(Opcodes.ARETURN)
                mv.visitMaxs(-1, -1) // auto-calculate stack size and number of locals
                mv.visitEnd()
                method.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "jyxal/Main",
                        methodName,
                        "()Ljava/lang/Object;",
                        false
                )
            }
            method.visitInsn(Opcodes.AASTORE)
        }
        method.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/list/JyxalList",
                "create",
                "([Ljava/lang/Object;)Lruntime/list/JyxalList;",
                false
        )
        AsmHelper.push(method)
    }

    override fun visitConstant(ctx: ConstantContext) {
        val mv = callStack.peek()
        mv.loadStack()
        Constants.compile(ctx.text, classWriter, mv)
        AsmHelper.push(mv)
    }

    override fun visitVariable_assn(ctx: Variable_assnContext) {
        val name = ctx.variable().text
        if (contextVariables.contains(name)) {
            val mv = callStack.peek()
            if (ctx.ASSN_SIGN().text == "→") {
                // set
                AsmHelper.pop(mv)
                mv.visitVarInsn(Opcodes.ASTORE, mv.ctxVar)
            } else {
                // get
                mv.loadStack()
                mv.visitVarInsn(Opcodes.ALOAD, mv.ctxVar)
                AsmHelper.push(mv)
            }
        } else {
            if (!variables.contains(name)) {
                variables.add(name)
                classWriter.visitField(
                        Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                        name,
                        "Ljava/lang/Object;",
                        null,
                        null
                )
                clinit.visitFieldInsn(
                        Opcodes.GETSTATIC,
                        "io/github/seggan/jyxal/runtime/math/BigComplex",
                        "ZERO",
                        "Lruntime/math/BigComplex;"
                )
                clinit.visitFieldInsn(
                        Opcodes.PUTSTATIC,
                        "jyxal/Main",
                        name,
                        "Ljava/lang/Object;"
                )
            }
            val mv = callStack.peek()
            if (ctx.ASSN_SIGN().text == "→") {
                // set
                AsmHelper.pop(mv)
                mv.visitFieldInsn(
                        Opcodes.PUTSTATIC,
                        "jyxal/Main",
                        name,
                        "Ljava/lang/Object;"
                )
            } else {
                // get
                mv.loadStack()
                mv.loadStack()
                mv.visitFieldInsn(
                        Opcodes.GETSTATIC,
                        "jyxal/Main",
                        name,
                        "Ljava/lang/Object;"
                )
                AsmHelper.push(mv)
            }
        }
    }

    override fun visitAlias(ctx: AliasContext) {
        aliases[ctx.theAlias.text] = ctx.program()
    }

    override fun visitElement(ctx: ElementContext) {
        var element = ctx.element_type().text
        if (ctx.PREFIX() != null) {
            element = ctx.PREFIX().text + element
        } else {
            val alias = aliases[element]
            if (alias != null) {
                visit(alias)
                return
            }
        }

        val consumer = if (ctx.MODIFIER() == null) null else visitModifier(ctx.MODIFIER().text)
        val mv = callStack.peek()

        if (element == "X") {
            val loop = loopStack.peek()
            mv.visitJumpInsn(Opcodes.GOTO, loop.end)
        } else {
            Constants.compile(element, classWriter, mv)
        }
        consumer?.accept(mv)
    }

    override fun visitWhile_loop(ctx: While_loopContext) {
        val start = Label()
        val end = Label()
        loopStack.push(Loop(start, end))
        val mv = callStack.peek()
        mv.reserveVar().use { ctxStore ->
            mv.visitVarInsn(Opcodes.ALOAD, mv.ctxVar)
            ctxStore.store()
            mv.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "runtime/math/BigComplex",
                    "ONE",
                    "Lruntime/math/BigComplex;"
            )
            mv.visitVarInsn(Opcodes.ASTORE, mv.ctxVar)
            mv.visitLabel(start)
            if (ctx.cond != null) {
                // we have a finite loop
                visit(ctx.cond)
                AsmHelper.pop(mv)
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeHelpers",
                        "truthValue",
                        "(Ljava/lang/Object;)Z",
                        false
                )
                mv.visitJumpInsn(Opcodes.IFEQ, end)
            }
            visit(ctx.body)
            mv.visitVarInsn(Opcodes.ALOAD, mv.ctxVar)
            mv.visitTypeInsn(Opcodes.CHECKCAST, "runtime/math/BigComplex")
            mv.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "runtime/math/BigComplex",
                    "ONE",
                    "Lruntime/math/BigComplex;"
            )
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "runtime/math/BigComplex",
                    "add",
                    "(Lruntime/math/BigComplex;)Lruntime/math/BigComplex;",
                    false
            )
            mv.visitVarInsn(Opcodes.ASTORE, mv.ctxVar)
            mv.visitJumpInsn(Opcodes.GOTO, start)
            mv.visitLabel(end)
            ctxStore.load()
            mv.visitVarInsn(Opcodes.ASTORE, mv.ctxVar)
        }
        loopStack.pop()
    }

    override fun visitFor_loop(ctx: For_loopContext) {
        if (ctx.variable() != null) {
            contextVariables.add(ctx.variable().text)
        }
        val start = Label()
        val end = Label()
        loopStack.push(Loop(start, end))
        generateFor(start, end, ctx.program())
        loopStack.pop()
    }

    override fun visitFori_loop(ctx: Fori_loopContext) {
        val start = Label()
        val end = Label()
        val mv = callStack.peek()
        loopStack.push(Loop(start, end))

        val num = ctx.DIGIT().joinToString("")

        if (ctx.program().getTokens(CONTEXT_VAR).isEmpty()) {
            // not freeing because this var is an int
            val counter = mv.reserveVar()
            AsmHelper.selectNumberInsn(mv, num.toInt())
            counter.store(Opcodes.ISTORE)
            mv.visitLabel(start)
            counter.load(Opcodes.ILOAD)
            mv.visitJumpInsn(Opcodes.IFEQ, end)
            visit(ctx.program())
            mv.visitIincInsn(counter.index, -1)
            mv.visitJumpInsn(Opcodes.GOTO, start)
            mv.visitLabel(end)
        } else {
            AsmHelper.addBigComplex(num, mv)
            generateFor(start, end, ctx.program())
        }
        loopStack.pop()
    }

    private fun generateFor(start: Label, end: Label, program: ProgramContext) {
        val mv = callStack.peek()
        AsmHelper.pop(mv)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "forify",
                "(Ljava/lang/Object;)Ljava/util/Iterator;",
                false
        )
        mv.reserveVar().use { iteratorVar ->
            mv.reserveVar().use { ctxStore ->
                mv.visitVarInsn(Opcodes.ALOAD, mv.ctxVar)
                ctxStore.store()
                iteratorVar.store()
                mv.visitLabel(start)
                iteratorVar.load()
                mv.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "java/util/Iterator",
                        "hasNext",
                        "()Z",
                        true
                )
                mv.visitJumpInsn(Opcodes.IFEQ, end)
                iteratorVar.load()
                mv.visitMethodInsn(
                        Opcodes.INVOKEINTERFACE,
                        "java/util/Iterator",
                        "next",
                        "()Ljava/lang/Object;",
                        true
                )
                mv.visitVarInsn(Opcodes.ASTORE, mv.ctxVar)
                visit(program)
                mv.visitJumpInsn(Opcodes.GOTO, start)
                mv.visitLabel(end)
                ctxStore.load()
                mv.visitVarInsn(Opcodes.ASTORE, mv.ctxVar)
            }
        }
    }

    override fun visitIf_statement(ctx: If_statementContext) {
        val mv = callStack.peek()
        val end = Label()
        loopStack.push(Loop(end, end))
        AsmHelper.pop(mv)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "truthValue",
                "(Ljava/lang/Object;)Z",
                false
        )
        mv.visitJumpInsn(Opcodes.IFEQ, end)
        visit(ctx.program(0))
        if (ctx.program().size > 1) {
            val elseEnd = Label()
            mv.visitJumpInsn(Opcodes.GOTO, elseEnd)
            mv.visitLabel(end)
            visit(ctx.program(1))
            mv.visitLabel(elseEnd)
        } else {
            mv.visitLabel(end)
        }
        loopStack.pop()
    }

    private fun visitModifier(modifier: String): Consumer<JyxalMethod>? {
        val mv = callStack.peek()
        // ß
        if ("ß" == modifier) {
            val end = Label()
            AsmHelper.pop(mv)
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "runtime/RuntimeHelpers",
                    "truthValue",
                    "(Ljava/lang/Object;)Z",
                    false
            )
            mv.visitJumpInsn(Opcodes.IFEQ, end)
            return Consumer { m: JyxalMethod -> m.visitLabel(end) }
        } else if ("&" == modifier) {
            mv.loadStack()
            mv.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "jyxal/Main",
                    "register",
                    "Ljava/lang/Object;"
            )
            AsmHelper.push(mv)
            return Consumer { m: JyxalMethod ->
                AsmHelper.pop(m)
                m.visitFieldInsn(
                        Opcodes.PUTSTATIC,
                        "jyxal/Main",
                        "register",
                        "Ljava/lang/Object;"
                )
            }
        }
        return null
    }

    override fun visitLambda(ctx: LambdaContext) {
        val lambdaName = "lambda$$lambdaCounter"
        var mv = classWriter.visitMethod(
                Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                lambdaName,
                "(Lruntime/ProgramStack;)Ljava/lang/Object;"
        )
        callStack.push(mv)
        visit(ctx.program())
        callStack.pop()
        AsmHelper.pop(mv)
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(-1, -1)
        mv.visitEnd()
        mv = callStack.peek()
        mv.visitTypeInsn(Opcodes.NEW, "runtime/Lambda")
        mv.visitInsn(Opcodes.DUP)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitLdcInsn(
                Handle(
                        Opcodes.H_INVOKESTATIC,
                        "jyxal/Main",
                        lambdaName,
                        "(Lruntime/ProgramStack;)Ljava/lang/Object;",
                        false
                )
        )
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "runtime/Lambda",
                "<init>",
                "(ILjava/lang/invoke/MethodHandle;)V",
                false
        )

        // normal lambda
        when (ctx.LAMBDA_TYPE().text) {
            "λ" -> {
                mv.visitTypeInsn(Opcodes.NEW, "runtime/Lambda")
                mv.visitInsn(Opcodes.DUP)
                AsmHelper.selectNumberInsn(mv, if (ctx.integer() == null) 1 else ctx.integer().text.toInt())
                mv.visitLdcInsn(
                        Handle(
                                Opcodes.H_INVOKESTATIC,
                                "jyxal/Main",
                                lambdaName,
                                "(Lruntime/ProgramStack;)Ljava/lang/Object;",
                                false
                        )
                )
                mv.visitMethodInsn(
                        Opcodes.INVOKESPECIAL,
                        "runtime/Lambda",
                        "<init>",
                        "(ILjava/lang/invoke/MethodHandle;)V",
                        false
                )
                mv.loadStack()
                mv.visitInsn(Opcodes.SWAP)
                AsmHelper.push(mv)
            }
            "ƛ" -> {
                AsmHelper.pop(mv)
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeHelpers",
                        "mapLambda",
                        "(Lruntime/Lambda;Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                )
                mv.loadStack()
                mv.visitInsn(Opcodes.SWAP)
                AsmHelper.push(mv)
            }
            "'" -> {
                AsmHelper.pop(mv)
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeHelpers",
                        "filterLambda",
                        "(Lruntime/Lambda;Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                )
                mv.loadStack()
                mv.visitInsn(Opcodes.SWAP)
                AsmHelper.push(mv)
            }
            "⟑" -> {
                AsmHelper.pop(mv)
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeHelpers",
                        "applyLambda",
                        "(Lruntime/Lambda;Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                )
                mv.loadStack()
                mv.visitInsn(Opcodes.SWAP)
                AsmHelper.push(mv)
            }
        }
        lambdaCounter++
    }

    override fun visitOne_element_lambda(ctx: One_element_lambdaContext) {
        visitLimitedLambda(listOf(ctx.program_node()))
    }

    override fun visitTwo_element_lambda(ctx: Two_element_lambdaContext) {
        visitLimitedLambda(ctx.program_node())
    }

    override fun visitThree_element_lambda(ctx: Three_element_lambdaContext) {
        visitLimitedLambda(ctx.program_node())
    }

    private fun visitLimitedLambda(nodes: List<Program_nodeContext>) {
        val lambdaName = "lambda$$lambdaCounter"
        var mv = classWriter.visitMethod(
                Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC,
                lambdaName,
                "(Lruntime/ProgramStack;)Ljava/lang/Object;"
        )
        callStack.push(mv)
        for (node in nodes) {
            visit(node)
        }
        callStack.pop()
        AsmHelper.pop(mv)
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(-1, -1)
        mv.visitEnd()
        mv = callStack.peek()
        mv.visitTypeInsn(Opcodes.NEW, "runtime/Lambda")
        mv.visitInsn(Opcodes.DUP)
        mv.visitInsn(Opcodes.ICONST_1)
        mv.visitLdcInsn(
                Handle(
                        Opcodes.H_INVOKESTATIC,
                        "jyxal/Main",
                        lambdaName,
                        "(Lruntime/ProgramStack;)Ljava/lang/Object;",
                        false
                )
        )
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "runtime/Lambda",
                "<init>",
                "(ILjava/lang/invoke/MethodHandle;)V",
                false
        )
        mv.loadStack()
        mv.visitInsn(Opcodes.SWAP)
        AsmHelper.push(mv)
    }

    override fun visitFunction(ctx: FunctionContext) {
        throw JyxalCompileException("Functions not yet supported")
    }

    private data class Loop(val start: Label, val end: Label)

    companion object {
        private val COMPLEX_SEPARATOR = Pattern.compile("°")

        fun compile(parser: VyxalParser, fileName: String?): ByteArray {
            val cw = JyxalClassWriter(ClassWriter.COMPUTE_FRAMES)
            cw.visit(Opcodes.V11, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "jyxal/Main", null, "java/lang/Object", null)

            // plus the register
            cw.visitField(
                    Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC,
                    "register",
                    "Ljava/lang/Object;",
                    null,
                    null
            ).visitEnd()
            cw.visitSource(fileName, null)
            val clinit = cw.visitMethod(
                    Opcodes.ACC_STATIC,
                    "<clinit>",
                    "()V",
                    null,
                    null
            )
            clinit.visitCode()
            clinit.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "io/github/seggan/jyxal/runtime/math/BigComplex",
                    "ZERO",
                    "Lio/github/seggan/jyxal/runtime/math/BigComplex;"
            )
            clinit.visitFieldInsn(Opcodes.PUTSTATIC, "jyxal/Main", "register", "Ljava/lang/Object;")
            val compiler = Compiler(cw, clinit)
            val main = cw.visitMethod(
                    Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                    "main",
                    "([Ljava/lang/String;)V"
            )
            compiler.callStack.push(main)
            main.visitCode()
            compiler.visit(parser.file())

            // finish up clinit
            clinit.visitInsn(Opcodes.RETURN)
            clinit.visitEnd()
            clinit.visitMaxs(0, 0)

            // TODO: reverse the signs for the variable assns

            // finish up main
            if (CompilerOptions.contains(CompilerOptions.PRINT_TO_FILE)) {
                main.loadStack()
                main.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeMethods",
                        "printToFile",
                        "(Lruntime/ProgramStack;)V",
                        false
                )
            } else {
                main.loadStack()
                main.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "runtime/ProgramStack",
                        "pop",
                        "()Ljava/lang/Object;",
                        false
                )
                main.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                main.visitInsn(Opcodes.SWAP)
                main.visitMethodInsn(
                        Opcodes.INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        "println",
                        "(Ljava/lang/Object;)V",
                        false
                )
            }
            main.visitInsn(Opcodes.RETURN)
            try {
                main.visitEnd()
                main.visitMaxs(0, 0)
            } catch (e: Exception) {
                FileOutputStream("debug.log").use { os ->
                    CheckClassAdapter.verify(
                            ClassReader(cw.toByteArray()),
                            true,
                            PrintWriter(os)
                    )
                }
                throw RuntimeException(e)
            }
            return cw.toByteArray()
        }
    }
}