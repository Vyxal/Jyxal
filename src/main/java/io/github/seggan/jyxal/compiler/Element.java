package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.CompilerOptions;
import io.github.seggan.jyxal.compiler.wrappers.ContextualVariable;
import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod;
import io.github.seggan.jyxal.runtime.ProgramStack;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum Element {

    /**
     * Math
     */
    ADD("+"),
    COMPLEMENT("\u2310", true),
    DIVIDE("/"),
    DOUBLE_REPEAT("d", true),
    HALVE("\u00BD"),
    INCREMENT("\u203A", true),
    INFINITE_PRIMES("\u00DEp", mv -> {
        mv.loadStack();
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeMethods",
                "infinitePrimes",
                "()Lruntime/list/JyxalList;",
                false
        );
        AsmHelper.push(mv);
    }),
    IS_PRIME("\u00E6", true),
    MODULO_FORMAT("%"),
    MULTI_COMMAND("\u2022"),
    MULTIPLY("*"),
    SUBTRACT("-"),
    SUM("\u2211", false),
    TWO_POW("E", true),

    /**
     * Boolean
     */
    ALL("A", false),
    BOOLIFY("\u1E03", mv -> {
        mv.loadStack();
        AsmHelper.pop(mv);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "truthValue",
                "(Ljava/lang/Object;)Z",
                false
        );
        mv.visitInsn(Opcodes.I2L);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(J)Lruntime/math/BigComplex;",
                false
        );
        AsmHelper.push(mv);
    }),
    EQUALS("="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL("\u2265"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("\u2264"),
    LOGICAL_AND("\u2227"),
    LOGICAL_NOT("\u00AC", mv -> {
        AsmHelper.pop(mv);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "truthValue",
                "(Ljava/lang/Object;)Z",
                false
        );
        Label elseStart = new Label();
        Label elseEnd = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, elseStart);
        mv.visitInsn(Opcodes.LCONST_0);
        mv.visitJumpInsn(Opcodes.GOTO, elseEnd);
        mv.visitLabel(elseStart);
        mv.visitInsn(Opcodes.LCONST_1);
        mv.visitLabel(elseEnd);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(J)Lruntime/math/BigComplex;",
                false
        );
        mv.loadStack();
        mv.visitInsn(Opcodes.SWAP);
        AsmHelper.push(mv);
    }),
    LOGICAL_OR("\u2228"),

    /**
     * String
     */
    CHR_ORD("C", true),
    INFINITE_REPLACE("\u00A2"),
    ITEM_SPLIT("\u00F7"),
    JOIN_BY_NEWLINES("\u204B", false),
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
    // inclusive one range
    IOR("\u027E", true),
    // inclusive zero range
    IZR("\u0280", true),
    LENGTH("L", false),
    MAP_GET_SET("\u00DEd"),
    MERGE("J"),
    PREPEND("p"),
    REMOVE_AT_INDEX("\u27C7"),
    SLICE_UNTIL("\u1E8E"),
    SORT_BY_FUNCTION("\u1E61"),
    STACK_SIZE("!", mv -> {
        mv.loadStack();
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "size",
                "()I",
                false
        );
        mv.visitInsn(Opcodes.I2L);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(J)Lruntime/math/BigComplex;",
                false
        );
        mv.loadStack();
        AsmHelper.push(mv);
    }),
    TAIL("t", false),

    /**
     * Stack
     */
    TRIPLICATE("D"),
    DUPLICATE(":", mv -> {
        AsmHelper.pop(mv);
        try (ContextualVariable obj = mv.reserveVar()) {
            obj.store();
            mv.loadStack();
            obj.load();
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "runtime/RuntimeHelpers",
                    "copy",
                    "(Ljava/lang/Object;)Ljava/lang/Object;",
                    false
            );
            AsmHelper.push(mv);
            mv.loadStack();
            obj.load();
            AsmHelper.push(mv);
        }
    }),
    POP("_", mv -> {
        AsmHelper.pop(mv);
        mv.visitInsn(Opcodes.POP);
    }),
    PUSH_REGISTER("\u00A5", mv -> {
        mv.loadStack();
        mv.visitFieldInsn(
                Opcodes.GETSTATIC,
                "jyxal/Main",
                "register",
                "Ljava/lang/Object;"
        );
        AsmHelper.push(mv);
    }),
    SET_REGISTER("\u00A3", mv -> {
        AsmHelper.pop(mv);
        mv.visitFieldInsn(
                Opcodes.PUTSTATIC,
                "jyxal/Main",
                "register",
                "Ljava/lang/Object;"
        );
    }),
    WRAP("W", mv -> {
        mv.loadStack();
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/list/JyxalList",
                "create",
                "(Lruntime/ProgramStack;)Lruntime/list/JyxalList;",
                false
        );
        mv.loadStack();
        mv.visitInsn(Opcodes.SWAP);
        AsmHelper.push(mv);
    }),

    /**
     * Misc
     */
    CONTEXT_VAR("n", mv -> {
        mv.loadStack();
        mv.loadContextVar();
        AsmHelper.push(mv);
    }),
    FUNCTION_CALL("\u2020"),
    GET_REQUEST("\u00A8U", true),
    INPUT("?", mv -> {
        mv.loadStack();
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "getInput",
                "()Ljava/lang/Object;",
                false
        );
        mv.loadStack();
        mv.visitInsn(Opcodes.SWAP);
        AsmHelper.push(mv);
    }),
    PRINT("\u20B4", mv -> {
        AsmHelper.pop(mv);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/Object;)V", false);
    }),
    PRINT_NO_POP("\u2026", mv -> {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.loadStack();
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "peek",
                "()Ljava/lang/Object;",
                false
        );
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/Object;)V", false);
    }),
    PRINTLN(",", mv -> {
        AsmHelper.pop(mv);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
    }),
    ;

    final LinkedMethodType type;

    final String text;
    private final BiConsumer<ClassWriter, JyxalMethod> compileMethod;

    Element(String text, Consumer<JyxalMethod> compileMethod) {
        this(text, (cw, mv) -> compileMethod.accept(mv));
    }

    Element(String text, BiConsumer<ClassWriter, JyxalMethod> compileMethod) {
        this.text = text;
        this.compileMethod = compileMethod;
        this.type = null;
    }

    Element(String text, LinkedMethodType type) {
        this.text = text;
        this.compileMethod = (cw, mv) -> {
            mv.loadStack();
            if (type.returnType != void.class) {
                mv.loadStack();
            }
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "runtime/RuntimeMethods",
                    screamingSnakeToCamel(name()),
                    Type.getMethodDescriptor(Type.getType(type.returnType), type.argType == Object.class ?
                            Type.getType(Object.class)
                            :
                            Type.getType("Lruntime/ProgramStack;")),
                    false
            );
            if (type.returnType != void.class) {
                AsmHelper.push(mv);
            }
        };
        this.type = type;
    }

    Element(String text) {
        this(text, LinkedMethodType.STACK_OBJECT);
    }

    Element(String text, String literal) {
        this(text, mv -> {
            mv.loadStack();
            mv.visitLdcInsn(literal);
            AsmHelper.push(mv);
        });
    }

    Element(String text, boolean vectorise) {
        this.text = text;
        String methodName = screamingSnakeToCamel(name());
        this.compileMethod = (cw, mv) -> {
            if (vectorise && !CompilerOptions.OPTIONS.contains(CompilerOptions.DONT_VECTORISE_MONADS)) {
                mv.loadStack();
                AsmHelper.pop(mv);
                mv.visitLdcInsn(new Handle(
                        Opcodes.H_INVOKESTATIC,
                        "runtime/RuntimeMethods",
                        methodName,
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                ));
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeMethods",
                        "vectorise",
                        "(Ljava/lang/Object;Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
                        false
                );
                AsmHelper.push(mv);
            } else {
                mv.loadStack();
                AsmHelper.pop(mv);
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/RuntimeMethods",
                        methodName,
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                );
                AsmHelper.push(mv);
            }
        };
        this.type = LinkedMethodType.OBJECT_OBJECT;
    }

    public static Element getByText(String text) {
        for (Element e : values()) {
            if (e.text.equals(text)) {
                return e;
            }
        }

        throw new JyxalCompileException("Unknown element: " + text);
    }

    static String screamingSnakeToCamel(String s) {
        StringBuilder sb = new StringBuilder();
        boolean wasUnderscore = false;
        for (char c : s.toCharArray()) {
            if (c == '_') {
                wasUnderscore = true;
            } else {
                if (wasUnderscore) {
                    sb.append(c);
                    wasUnderscore = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }

        return sb.toString();
    }

    public void compile(ClassWriter cw, JyxalMethod mv) {
        compileMethod.accept(cw, mv);
    }

    enum LinkedMethodType {
        OBJECT_OBJECT(Object.class, Object.class),
        OBJECT_VOID(Object.class, void.class),
        STACK_VOID(ProgramStack.class, void.class),
        STACK_OBJECT(ProgramStack.class, Object.class);

        final Class<?> returnType;
        final Class<?> argType;

        LinkedMethodType(Class<?> argType, Class<?> returnType) {
            this.returnType = returnType;
            this.argType = argType;
        }
    }
}
