package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum Element {

    ADD("+"),
    ASTERISK("\u00D7", "*"),
    SPLIT_ON("\u20AC"),
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
    HALVE("\u00BD"),
    ALL("A", false),
    CHR_ORD("C", true),
    TRIPLICATE("D"),
    TWO_POW("E", true),
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
    INFINITE_PRIMES("\u00DEp"),
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
    ;

    // used for unit testing
    final boolean isLinkedToMethod;

    final String text;
    private final BiConsumer<ClassWriter, JyxalMethod> compileMethod;

    Element(String text, Consumer<JyxalMethod> compileMethod) {
        this(text, (cw, mv) -> compileMethod.accept(mv));
    }

    Element(String text, BiConsumer<ClassWriter, JyxalMethod> compileMethod) {
        this.text = text;
        this.compileMethod = compileMethod;
        this.isLinkedToMethod = false;
    }

    Element(String text) {
        this.text = text;
        this.compileMethod = (cw, mv) -> {
            mv.loadStack();
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "runtime/RuntimeMethods",
                    screamingSnakeToCamel(name()),
                    "(Lruntime/ProgramStack;)V",
                    false
            );
        };
        this.isLinkedToMethod = true;
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
                        "runtime/MonadicFunctions",
                        methodName,
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                ));
                mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        "runtime/MonadicFunctions",
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
                        "runtime/MonadicFunctions",
                        methodName,
                        "(Ljava/lang/Object;)Ljava/lang/Object;",
                        false
                );
                AsmHelper.push(mv);
            }
        };
        this.isLinkedToMethod = true;
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
}
