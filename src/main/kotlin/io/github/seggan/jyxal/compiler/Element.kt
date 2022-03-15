package io.github.seggan.jyxal.compiler

import io.github.seggan.jyxal.CompilerOptions
import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod
import io.github.seggan.jyxal.runtime.ProgramStack
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.reflect.KClass

@Suppress("unused")
enum class Element {
    /**
     * Math
     */
    ADD("+"),
    COMPLEMENT("\u2310", true),
    DIVIDE("/"),
    DOUBLE_REPEAT("d", true),
    HALVE("\u00BD"),
    INCREMENT(
            "\u203A",
            true
    ),
    INFINITE_PRIMES("\u00DEp", Consumer { mv: JyxalMethod ->
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
    IS_PRIME("\u00E6", true),
    MODULO_FORMAT("%"),
    MULTI_COMMAND("\u2022"),
    MULTIPLY("*"),
    SUBTRACT("-"),
    SUM(
            "\u2211",
            false
    ),
    TWO_POW("E", true),

    /**
     * Boolean
     */
    ALL("A", false),
    BOOLIFY("\u1E03", Consumer { mv: JyxalMethod ->
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
    GREATER_THAN_OR_EQUAL("\u2265"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("\u2264"),
    LOGICAL_AND(
            "\u2227"
    ),
    LOGICAL_NOT("\u00AC", Consumer { mv: JyxalMethod ->
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
    LOGICAL_OR("\u2228"),

    /**
     * String
     */
    CHR_ORD("C", true),
    INFINITE_REPLACE("\u00A2"),
    ITEM_SPLIT("\u00F7"),
    JOIN_BY_NEWLINES(
            "\u204B",
            false
    ),
    JOIN_BY_NOTHING("\u1E45", false),
    JSON_PARSE("\u00F8J", true),
    REVERSE("\u1E58", false),
    SPLIT_ON("\u20AC"),

    /**
     * Literals
     */
    ASTERISK("\u00D7", "*"),
    SPACE("\u00F0", " "),

    /**
     * List
     */
    FLATTEN("f", false),
    HEAD("h", false),
    HEAD_EXTRACT("\u1E23"),
    IOR("\u027E", true), // inclusive one range
    IZR("\u0280", true), // inclusive zero range
    LENGTH(
            "L",
            false
    ),
    MAP_GET_SET("\u00DEd"),
    MERGE("J"),
    PREPEND("p"),
    REMOVE_AT_INDEX("\u27C7"),
    SLICE_UNTIL("\u1E8E"), SORT_BY_FUNCTION(
            "\u1E61"
    ),
    STACK_SIZE("!", Consumer { mv: JyxalMethod ->
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
    TAIL("t", false),

    /**
     * Stack
     */
    TRIPLICATE("D"),
    DUPLICATE(":", Consumer { mv: JyxalMethod ->
        AsmHelper.pop(mv)
        mv.reserveVar().use { obj ->
            obj.store()
            mv.loadStack()
            obj.load()
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "runtime/RuntimeHelpers",
                    "copy",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    false
            )
            AsmHelper.push(mv)
            mv.loadStack()
            obj.load()
            AsmHelper.push(mv)
        }
    }),
    POP("_", Consumer { mv: JyxalMethod ->
        AsmHelper.pop(mv)
        mv.visitInsn(Opcodes.POP)
    }),
    PUSH_REGISTER("\u00A5", Consumer { mv: JyxalMethod ->
        mv.loadStack()
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "jyxal/Main",
                "register",
                "Ljava/lang/Object;"
        )
        AsmHelper.push(mv)
    }),
    SET_REGISTER("\u00A3", Consumer { mv: JyxalMethod ->
        AsmHelper.pop(mv)
        mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                "jyxal/Main",
                "register",
                "Ljava/lang/Object;"
        )
    }),
    WRAP("W", Consumer { mv: JyxalMethod ->
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
    CONTEXT_VAR("n", Consumer { mv: JyxalMethod ->
        mv.loadStack()
        mv.loadContextVar()
        AsmHelper.push(mv)
    }),
    FUNCTION_CALL("\u2020"),
    GET_REQUEST("\u00A8U", true),
    INPUT("?", Consumer { mv: JyxalMethod ->
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
    PRINT("\u20B4", Consumer { mv: JyxalMethod ->
        AsmHelper.pop(mv)
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        mv.visitInsn(Opcodes.SWAP)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/Object;)V", false)
    }),
    PRINT_NO_POP("\u2026", Consumer { mv: JyxalMethod ->
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
    PRINTLN(",", Consumer { mv: JyxalMethod ->
        AsmHelper.pop(mv)
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
        mv.visitInsn(Opcodes.SWAP)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false)
    });

    @JvmField
    val type: LinkedMethodType?
    val text: String
    private val compileMethod: BiConsumer<ClassWriter, JyxalMethod>

    constructor(text: String, compileMethod: Consumer<JyxalMethod>) : this(
            text,
            BiConsumer<ClassWriter, JyxalMethod> { _: ClassWriter, mv: JyxalMethod -> compileMethod.accept(mv) }
    )

    constructor(text: String, compileMethod: BiConsumer<ClassWriter, JyxalMethod>) {
        this.text = text
        this.compileMethod = compileMethod
        this.type = null
    }

    constructor(text: String, type: LinkedMethodType = LinkedMethodType.STACK_OBJECT) {
        this.text = text
        compileMethod = BiConsumer { _: ClassWriter, mv: JyxalMethod ->
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

    constructor(text: String, literal: String) : this(text, Consumer<JyxalMethod> { mv: JyxalMethod ->
        mv.loadStack()
        mv.visitLdcInsn(literal)
        AsmHelper.push(mv)
    })

    constructor(text: String, vectorise: Boolean) {
        this.text = text
        val methodName = screamingSnakeToCamel(name)
        compileMethod = BiConsumer { _: ClassWriter?, mv: JyxalMethod ->
            if (vectorise && !CompilerOptions.OPTIONS.contains(CompilerOptions.DONT_VECTORISE_MONADS)) {
                mv.loadStack()
                AsmHelper.pop(mv)
                mv.visitLdcInsn(
                        Handle(
                                Opcodes.H_INVOKESTATIC,
                                "runtime/RuntimeMethods",
                                methodName,
                                "(Ljava/lang/Object;)Ljava/lang/Object;",
                                false
                        )
                )
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeMethods",
                        "vectorise",
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
        compileMethod.accept(cw, mv)
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