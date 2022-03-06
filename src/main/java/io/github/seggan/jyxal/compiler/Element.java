package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod;
import io.github.seggan.jyxal.runtime.ProgramStack;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum Element {

    ADD("+"),
    ASTERISK("\u00D7", "*"),
    SPLIT_ON("\u20AC"),
    REMOVE_AT_INDEX("\u27C7"),
    INFINITE_REPLACE("\u00A2"),
    COMPLEMENT("\u2310", true),
    // inclusive zero range
    IZR("\u0280", true),
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
    MULTI_COMMAND("\u2022"),
    FUNCTION_CALL("\u2020"),
    MULTIPLY("*"),
    SUM("\u2211", false),
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
    MODULO_FORMAT("%"),
    HEAD("h", false),
    TAIL("t", false),
    INCREMENT("\u203A", true),
    HALVE("\u00BD"),
    ALL("A", false),
    CHR_ORD("C", true),
    TRIPLICATE("D"),
    TWO_POW("E", true),
    FLATTEN("f", false),
    DUPLICATE(":"),
    EQUALS("="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL("\u2265"),
    ITEM_SPLIT("\u00F7"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("\u2264"),
    LOGICAL_AND("\u2227"),
    LOGICAL_OR("\u2228"),
    CONTEXT_VAR("n", mv -> {
        mv.loadStack();
        mv.loadContextVar();
        AsmHelper.push(mv);
    }),
    IS_PRIME("\u00E6", true),
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
    POP("_", mv -> {
        AsmHelper.pop(mv);
        mv.visitInsn(Opcodes.POP);
    }),
    PRINTLN(",", mv -> {
        AsmHelper.pop(mv);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
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
    GET_REQUEST("\u00A8U", true),
    JSON_PARSE("\u00F8J", true),
    MAP_GET_SET("\u00DEd"),
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
            if (vectorise) {
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
