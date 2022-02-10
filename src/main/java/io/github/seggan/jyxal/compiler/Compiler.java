package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.antlr.VyxalBaseVisitor;
import io.github.seggan.jyxal.antlr.VyxalParser;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/*
Vars:
0: args (if main)
1: stack size
2: context variable
3-5: for program use
6+: Jyxal variables
 */
@SuppressWarnings("ConstantConditions")
public final class Compiler extends VyxalBaseVisitor<Void> implements Opcodes {

    private static final Pattern COMPLEX_SEPARATOR = Pattern.compile("°");

    final VyxalParser parser;
    final ClassWriter classWriter;

    // must be LinkedHashSet to preserve order
    final Set<String> variables = new LinkedHashSet<>();
    private final Set<String> contextVariables = new HashSet<>();

    private final Deque<MethodVisitorWrapper> callStack = new ArrayDeque<>();

    private int listCounter = 0;

    private Compiler(VyxalParser parser, ClassWriter classWriter) {
        this.parser = parser;
        this.classWriter = classWriter;
    }

    public static byte[] compile(VyxalParser parser, String fileName) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC + ACC_FINAL, "jyxal/Main", null, "java/lang/Object", null);
        cw.visitSource(fileName, null);

        Compiler compiler = new Compiler(parser, cw);

        MethodVisitorWrapper main = new MethodVisitorWrapper(cw.visitMethod(
            ACC_PUBLIC | ACC_STATIC,
            "main",
            "([Ljava/lang/String;)V",
            null,
            null
        ), 1, 2);
        compiler.callStack.push(main);
        main.visitCode();

        compiler.visit(parser.file());

        Label end = new Label();
        main.visitVarInsn(ILOAD, main.getStackVar());
        main.visitJumpInsn(IFEQ, end);

        main.visitInsn(DUP); // keep the verifier from complaining

        main.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        main.visitInsn(SWAP);
        main.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);

        main.visitFrame(F_SAME, 0, null, 0, null);
        main.visitLabel(end);
        main.visitInsn(RETURN);

        main.visitMaxs(-1, -1); // auto-calculate stack size and number of locals
        main.visitEnd();

        return cw.toByteArray();
    }

    @Override
    public Void visitInteger(VyxalParser.IntegerContext ctx) {
        MethodVisitorWrapper mv = callStack.peek();
        AsmHelper.addBigComplex(ctx.getText(), mv);
        return null;
    }

    @Override
    public Void visitComplex(VyxalParser.ComplexContext ctx) {
        MethodVisitorWrapper mv = callStack.peek();
        String[] parts = COMPLEX_SEPARATOR.split(ctx.getText());
        AsmHelper.addBigDecimal(parts[0], mv);
        AsmHelper.addBigDecimal(parts[1], mv);
        mv.visitMethodInsn(
            INVOKESTATIC,
            "runtime/math/BigComplex",
            "valueOf",
            "(Ljava/math/BigDecimal;Ljava/math/BigDecimal;)Lruntime/math/BigComplex;",
            false
        );
        mv.visitIincInsn(mv.getStackVar(), -1);

        return null;
    }

    @Override
    public Void visitNormal_string(VyxalParser.Normal_stringContext ctx) {
        MethodVisitorWrapper mv = callStack.peek();
        mv.visitLdcInsn(ctx.any_text().getText());
        mv.visitIincInsn(mv.getStackVar(), 1);
        return null;
    }

    @Override
    public Void visitSingle_char_string(VyxalParser.Single_char_stringContext ctx) {
        MethodVisitorWrapper mv = callStack.peek();
        mv.visitLdcInsn(ctx.getText().substring(1));
        mv.visitIincInsn(mv.getStackVar(), 1);
        return null;
    }

    @Override
    public Void visitDouble_char_string(VyxalParser.Double_char_stringContext ctx) {
        MethodVisitorWrapper mv = callStack.peek();
        mv.visitLdcInsn(ctx.getText().substring(1));
        mv.visitIincInsn(mv.getStackVar(), 1);
        return null;
    }

    @Override
    public Void visitList(VyxalParser.ListContext ctx) {
        MethodVisitorWrapper method = callStack.peek();
        method.visitLdcInsn(ctx.program().size());
        method.visitTypeInsn(ANEWARRAY, "java/lang/Object");

        List<VyxalParser.ProgramContext> program = ctx.program();
        for (int i = 0; i < program.size(); i++) {
            VyxalParser.ProgramContext item = program.get(i);
            method.visitInsn(DUP);
            AsmHelper.selectNumberInsn(method, i);

            String methodName = "listInit$" + listCounter++;
            MethodVisitorWrapper mv = new MethodVisitorWrapper(classWriter.visitMethod(
                ACC_PRIVATE | ACC_STATIC,
                methodName,
                "()Ljava/lang/Object;",
                null,
                null
            ), 1, 2);
            mv.visitCode();
            callStack.push(mv);
            visit(item);
            callStack.pop();
            mv.visitInsn(ARETURN);
            mv.visitMaxs(-1, -1); // auto-calculate stack size and number of locals
            mv.visitEnd();

            method.visitMethodInsn(
                INVOKESTATIC,
                "jyxal/Main",
                methodName,
                "()Ljava/lang/Object;",
                false
            );

            method.visitInsn(AASTORE);
        }

        method.visitMethodInsn(
            INVOKESTATIC,
            "runtime/list/JyxalList",
            "create",
            "([Ljava/lang/Object;)Lruntime/list/JyxalList;",
            false
        );
        method.visitIincInsn(method.getStackVar(), 1);

        return null;
    }

}
