package io.github.seggan.jyxal.compiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.function.BiConsumer;

public enum Element implements Opcodes {
    ADD("+", (cv, mv) ->  mv.visitMethodInsn(INVOKESTATIC,
            "runtime/MathMethods",
            "add",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
            false
        ))
    ;

    private final String text;
    private final BiConsumer<ClassWriter, MethodVisitor> consumer;

    Element(String text, BiConsumer<ClassWriter, MethodVisitor> consumer) {
        this.text = text;
        this.consumer = consumer;
    }

    public void compile(ClassWriter cw, MethodVisitor mv) {
        consumer.accept(cw, mv);
    }

    public static Element getByText(String text) {
        for (Element e : values()) {
            if (e.text.equals(text)) {
                return e;
            }
        }

        throw new JyxalCompileException("Unknown element: " + text);
    }
}
