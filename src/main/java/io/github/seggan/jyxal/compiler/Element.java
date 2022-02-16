package io.github.seggan.jyxal.compiler;

import com.google.common.base.CaseFormat;
import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod;
import io.github.seggan.jyxal.runtime.RuntimeMethods;
import io.github.seggan.jyxal.runtime.ProgramStack;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum Element {

    ADD("+"),
    ALL("A"),
    CHR_ORD("C"),
    TRIPLICATE("D"),
    TWO_POW("E"),
    DUPLICATE(":"),
    EQUALS("="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL("≥"),
    ITEM_SPLIT("÷"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("≤"),
    LOGICAL_AND("∧"),
    LOGICAL_OR("∨"),
    CONTEXT_VAR("n", mv -> {
        mv.visitVarInsn(Opcodes.ALOAD, mv.getStackVar());
        mv.visitVarInsn(Opcodes.ALOAD, mv.getCtxVar());
        AsmHelper.push(mv);
    }),
    POP("_", mv -> {
        AsmHelper.pop(mv);
        mv.visitInsn(Opcodes.POP);
    }),
    PRINTLN(",", mv -> {
        // Unfortunately, we cannot apply the push-pop optimization here as it does not push anything
        // to the stack. Therefore, the program stack remains on the operand stack, and we'll get an
        // inconsistent frame error.
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        AsmHelper.pop(mv);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
    }),
    ;

    private final String text;
    private final BiConsumer<ClassWriter, JyxalMethod> compileMethod;

    Element(String text, Consumer<JyxalMethod> compileMethod) {
        this(text, (cw, mv) -> compileMethod.accept(mv));
    }

    Element(String text, BiConsumer<ClassWriter, JyxalMethod> compileMethod) {
        this.text = text;
        this.compileMethod = compileMethod;
    }

    Element(String text) {
        this.text = text;

        String method = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());
        try {
            RuntimeMethods.class.getDeclaredMethod(method, ProgramStack.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No such method: RuntimeMethods#" + method, e);
        }

        this.compileMethod = (cw, mv) -> {
            mv.visitVarInsn(Opcodes.ALOAD, mv.getStackVar());
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "runtime/RuntimeMethods",
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

    public void compile(ClassWriter cw, JyxalMethod mv) {
        compileMethod.accept(cw, mv);
    }
}
