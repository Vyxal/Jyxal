package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.runtime.MathMethods;
import io.github.seggan.jyxal.runtime.ProgramStack;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public enum Element {

    ADD("+", MathMethods.class, "add"),
    CONTEXT_VAR("n", mv -> {
        mv.visitVarInsn(Opcodes.ALOAD, mv.getStackVar());
        mv.visitVarInsn(Opcodes.ALOAD, mv.getCtxVar());
        AsmHelper.push(mv);
    }),
    POP("_", mv -> {
        AsmHelper.pop(mv);
        mv.visitInsn(Opcodes.POP);
    });

    private final String text;
    private final BiConsumer<ClassWriter, MethodVisitorWrapper> compileMethod;

    Element(String text, Consumer<MethodVisitorWrapper> compileMethod) {
        this(text, (cw, mv) -> compileMethod.accept(mv));
    }

    Element(String text, BiConsumer<ClassWriter, MethodVisitorWrapper> compileMethod) {
        this.text = text;
        this.compileMethod = compileMethod;
    }

    Element(String text, Class<?> owner, String method) {
        this.text = text;

        try {
            owner.getDeclaredMethod(method, ProgramStack.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No such method: " + owner.getName() + "#" + method, e);
        }

        Pattern oldPackage = Pattern.compile("io/github/seggan/jyxal/runtime/");
        this.compileMethod = (cw, mv) -> {
            mv.visitVarInsn(Opcodes.ALOAD, mv.getStackVar());
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                oldPackage.matcher(Type.getType(owner).getInternalName()).replaceAll("runtime/"),
                method,
                "(Lruntime/ProgramStack;)V",
                false
            );
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
        compileMethod.accept(cw, mv);
    }
}
