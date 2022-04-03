package io.github.seggan.jyxal.compiler

import io.github.seggan.jyxal.CompilerOptions
import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod
import io.github.seggan.jyxal.runtime.ProgramStack
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import kotlin.reflect.KClass

@Suppress("unused")
enum class Element {

    /**
     * Math
     */
    ADD("+"),
    COMPLEMENT("⌐", true),
    DIVIDE("/"),
    DOUBLE_REPEAT("d", true),
    HALVE("½"),
    HEX_TO_DECIMAL("H", true),
    INCREMENT("›", true),
    INFINITE_PRIMES("Þp", { mv ->
        mv.loadStack()
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeMethods",
                "infinitePrimes",
                "()Lruntime/list/JyxalList;",
                false
        )
        AsmHelper.push(mv)
    }),
    IS_PRIME("æ", true),
    MODULO_FORMAT("%"),
    MULTI_COMMAND("•"),
    MULTIPLY("*"),
    SUBTRACT("-"),
    SUM("∑", false),
    TWO_POW("E", true),

    /**
     * Boolean
     */
    ALL("A", false),
    BOOLIFY("ḃ", { mv ->
        mv.loadStack()
        AsmHelper.pop(mv)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "truthValue",
                "(Ljava/lang/Object;)Z",
                false
        )
        mv.visitInsn(Opcodes.I2L)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(J)Lruntime/math/BigComplex;",
                false
        )
        AsmHelper.push(mv)
    }),
    EQUAL("="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL("≥"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("≤"),
    LOGICAL_AND("∧"),
    LOGICAL_NOT("¬", { mv ->
        AsmHelper.pop(mv)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "truthValue",
                "(Ljava/lang/Object;)Z",
                false
        )
        val elseStart = Label()
        val elseEnd = Label()
        mv.visitJumpInsn(Opcodes.IFEQ, elseStart)
        mv.visitInsn(Opcodes.LCONST_0)
        mv.visitJumpInsn(Opcodes.GOTO, elseEnd)
        mv.visitLabel(elseStart)
        mv.visitInsn(Opcodes.LCONST_1)
        mv.visitLabel(elseEnd)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(J)Lruntime/math/BigComplex;",
                false
        )
        mv.loadStack()
        mv.visitInsn(Opcodes.SWAP)
        AsmHelper.push(mv)
    }),
    LOGICAL_OR("∨"),

    /**
     * String
     */
    CHR_ORD("C", true),
    INFINITE_REPLACE("¢"),
    ITEM_SPLIT("÷"),
    JOIN_BY_NEWLINES("⁋", false),
    JOIN_BY_NOTHING("ṅ", false),
    JSON_PARSE("øJ", true),
    REVERSE("Ṙ", false),
    SPACES("I", false),
    SPLIT_ON("€"),

    /**
     * Literals
     */
    ASTERISK("×", "*"),
    SPACE("ð", " "),

    /**
     * List
     */
    FILTER("F"),
    FLATTEN("f", false),
    HEAD("h", false),
    HEAD_EXTRACT("ḣ"),
    IOR("ɾ", true), // inclusive one range
    IZR("ʀ", true), // inclusive zero range
    LENGTH("L", false),
    MAP_GET_SET("Þd"),
    MAX("G", false),
    MERGE("J"),
    PREPEND("p"),
    REMOVE_AT_INDEX("⟇"),
    SLICE_UNTIL("Ẏ"),
    SORT_BY_FUNCTION("ṡ"),
    TAIL("t", false),

    /**
     * Stack
     */
    TRIPLICATE("D"),
    DUPLICATE(":", { mv ->
        AsmHelper.pop(mv)
        mv.visitInsn(Opcodes.DUP)
        mv.loadStack()
        mv.visitInsn(Opcodes.SWAP)
        AsmHelper.push(mv)
        mv.loadStack()
        mv.visitInsn(Opcodes.SWAP)
        AsmHelper.push(mv)
    }),
    POP("_", { mv ->
        AsmHelper.pop(mv)
        mv.visitInsn(Opcodes.POP)
    }),
    PUSH_REGISTER("¥", { mv ->
        mv.loadStack()
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "jyxal/Main",
                "register",
                "Ljava/lang/Object;"
        )
        AsmHelper.push(mv)
    }),
    SET_REGISTER("£", { mv ->
        AsmHelper.pop(mv)
        mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                "jyxal/Main",
                "register",
                "Ljava/lang/Object;"
        )
    }),
    STACK_SIZE("!", { mv ->
        mv.loadStack()
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "size",
                "()I",
                false
        )
        mv.visitInsn(Opcodes.I2L)
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(J)Lruntime/math/BigComplex;",
                false
        )
        mv.loadStack()
        AsmHelper.push(mv)
    }),
    WRAP("W", { mv ->
        mv.loadStack()
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/list/JyxalList",
                "create",
                "(Lruntime/ProgramStack;)Lruntime/list/JyxalList;",
                false
        )
        mv.loadStack()
        mv.visitInsn(Opcodes.SWAP)
        AsmHelper.push(mv)
    }),

    /**
     * Misc
     */
    CONTEXT_VAR("n", { mv ->
        mv.loadStack()
        mv.loadContextVar()
        AsmHelper.push(mv)
    }),
    FUNCTION_CALL("†"),
    GET_REQUEST("¨U", true),
    INPUT("?", { mv ->
        mv.loadStack()
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "getInput",
                "()Ljava/lang/Object;",
                false
        )
        mv.loadStack()
        mv.visitInsn(Opcodes.SWAP)
        AsmHelper.push(mv)
    }),
    PRINT("₴", { mv ->
        AsmHelper.pop(mv)
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        mv.visitInsn(Opcodes.SWAP)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/Object;)V", false)
    }),
    PRINT_NO_POP("…", { mv ->
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        mv.loadStack()
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "peek",
                "()Ljava/lang/Object;",
                false
        )
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/Object;)V", false)
    }),
    PRINTLN(",", { mv ->
        AsmHelper.pop(mv)
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        mv.visitInsn(Opcodes.SWAP)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false)
    });

    @JvmField
    val type: LinkedMethodType?
    val text: String
    private val compileMethod: (ClassWriter, JyxalMethod) -> Unit

    constructor(text: String, compileMethod: (JyxalMethod) -> Unit) : this(
            text,
            { _, mv -> compileMethod(mv) }
    )

    constructor(text: String, compileMethod: (ClassWriter, JyxalMethod) -> Unit) {
        this.text = text
        this.compileMethod = compileMethod
        this.type = null
    }

    constructor(text: String, type: LinkedMethodType = LinkedMethodType.STACK_OBJECT) {
        this.text = text
        compileMethod = { _, mv ->
            mv.loadStack()
            if (type.returnType != Void.TYPE) {
                mv.loadStack()
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "runtime/RuntimeMethods",
                    screamingSnakeToCamel(name),
                    Type.getMethodDescriptor(
                            Type.getType(type.returnType.java),
                            if (type.argType == Any::class) Type.getType(Any::class.java) else Type.getType("Lruntime/ProgramStack;")
                    ),
                    false
            )
            if (type.returnType != Void.TYPE) {
                AsmHelper.push(mv)
            }
        }
        this.type = type
    }

    constructor(text: String, literal: String) : this(text, { mv ->
        mv.loadStack()
        mv.visitLdcInsn(literal)
        AsmHelper.push(mv)
    })

    constructor(text: String, vectorise: Boolean) {
        this.text = text
        val methodName = screamingSnakeToCamel(name)
        compileMethod = { _, mv ->
            if (vectorise && !CompilerOptions.OPTIONS.contains(CompilerOptions.DONT_VECTORISE_MONADS)) {
                mv.loadStack()
                AsmHelper.pop(mv)
                mv.visitLdcInsn(
                        Handle(
                                Opcodes.H_INVOKESTATIC,
                                "io/github/seggan/jyxal/runtime/RuntimeMethods",
                                methodName,
                                "(Ljava/lang/Object;)Ljava/lang/Object;",
                                false
                        )
                )
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeMethods",
                        "monadVectorise",
                        "(Ljava/lang/Object;Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
                        false
                )
                AsmHelper.push(mv)
            } else {
                mv.loadStack()
                AsmHelper.pop(mv)
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeMethods",
                        methodName,
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                )
                AsmHelper.push(mv)
            }
        }
        type = LinkedMethodType.OBJECT_OBJECT
    }

    fun compile(cw: ClassWriter, mv: JyxalMethod) {
        compileMethod(cw, mv)
    }

    enum class LinkedMethodType(val argType: KClass<*>, val returnType: KClass<*>) {
        OBJECT_OBJECT(Any::class, Any::class),
        OBJECT_VOID(Any::class, Void::class),
        STACK_VOID(ProgramStack::class, Void::class),
        STACK_OBJECT(ProgramStack::class, Any::class);
    }

    companion object {
        fun getByText(text: String): Element {
            for (e in values()) {
                if (e.text == text) {
                    return e
                }
            }
            throw JyxalCompileException("Unknown element: $text")
        }
    }
}

fun screamingSnakeToCamel(s: String): String {
    val sb = StringBuilder()
    var wasUnderscore = false
    for (c in s.toCharArray()) {
        if (c == '_') {
            wasUnderscore = true
        } else {
            if (wasUnderscore) {
                sb.append(c)
                wasUnderscore = false
            } else {
                sb.append(c.lowercaseChar())
            }
        }
    }
    return sb.toString()
}