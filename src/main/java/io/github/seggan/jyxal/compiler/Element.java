package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.runtime.MathMethods;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

public enum Element {

    ADD("+", -1, MathMethods.class, "add", Object.class, Object.class, Object.class),
    CONTEXT_VAR("n", 1, (cv, mv) -> mv.visitVarInsn(Opcodes.ALOAD, mv.getCtxVar())),
    POP("_", -1, (cw, mv) -> {
        mv.visitInsn(Opcodes.POP);
    });

    private final String text;
    private final int stackDelta;
    private final BiConsumer<ClassWriter, MethodVisitorWrapper> consumer;

    Element(String text, int stackDelta, BiConsumer<ClassWriter, MethodVisitorWrapper> consumer) {
        BiConsumer<ClassWriter, MethodVisitorWrapper> consumer1;
        this.text = text;
        this.stackDelta = stackDelta;
        if (stackDelta != 0) {
            consumer = consumer.andThen((cw, mv) -> mv.visitIincInsn(mv.getStackVar(), stackDelta));
        }
        this.consumer = consumer;
    }

    Element(String text, int stackDelta, Class<?> owner, String method, Class<?> returnType, Class<?>... parameterTypes) {
        this.text = text;
        this.stackDelta = stackDelta;

        MethodType methodType = MethodType.methodType(returnType, parameterTypes);
        try {
            MethodHandles.lookup().findStatic(owner, method, methodType);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalArgumentException("No such method: " + owner.getName() + "#" + method, e);
        }

        Pattern oldPackage = Pattern.compile("io/github/seggan/jyxal/runtime/");
        this.consumer = (cw, mv) -> {
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                oldPackage.matcher(Type.getType(owner).getInternalName()).replaceAll("runtime/"),
                method,
                oldPackage.matcher(methodType.toMethodDescriptorString()).replaceAll("runtime/"),
                false
            );
            mv.visitIincInsn(mv.getStackVar(), stackDelta);
        };
    }

    public static Element getByText(String text) {
        for (Element e : values()) {
            if (e.text.equals(text)) {
                return e;
            }
        }

        throw new JyxalCompileException("Unknown element: " + text);
    }

    public void compile(ClassWriter cw, MethodVisitorWrapper mv) {
        consumer.accept(cw, mv);
    }
}
