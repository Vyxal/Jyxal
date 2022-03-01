package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.antlr.VyxalParser;
import io.github.seggan.jyxal.antlr.VyxalParserBaseVisitor;
import io.github.seggan.jyxal.compiler.wrappers.ContextualVariable;
import io.github.seggan.jyxal.compiler.wrappers.JyxalClassWriter;
import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

@SuppressWarnings("ConstantConditions")
public final class Compiler extends VyxalParserBaseVisitor<Void> implements Opcodes {

    private static final Pattern COMPLEX_SEPARATOR = Pattern.compile("\u00B0");
    private static final Pattern RUNTIME = Pattern.compile("^runtime\\.");

    final VyxalParser parser;
    final JyxalClassWriter classWriter;
    private final MethodVisitor clinit;

    private final Set<String> variables = new HashSet<>();
    private final Set<String> contextVariables = new HashSet<>();

    private final Deque<JyxalMethod> callStack = new ArrayDeque<>();

    private int listCounter = 0;
    private int lambdaCounter = 0;

    private Compiler(VyxalParser parser, JyxalClassWriter classWriter, MethodVisitor clinit) {
        this.parser = parser;
        this.classWriter = classWriter;
        this.clinit = clinit;
    }

    public static byte[] compile(VyxalParser parser, String fileName) {
        JyxalClassWriter cw = new JyxalClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC + ACC_FINAL, "jyxal/Main", null, "java/lang/Object", null);

        // add the register
        cw.visitField(
                ACC_PRIVATE + ACC_STATIC + ACC_FINAL,
                "register",
                "Ljava/lang/Object;",
                null,
                null
        ).visitEnd();

        cw.visitSource(fileName, null);

        MethodVisitor clinit = cw.visitMethod(
                ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
        );
        clinit.visitCode();

        clinit.visitFieldInsn(
                GETSTATIC,
                "runtime/math/BigComplex",
                "ZERO",
                "Lruntime/math/BigComplex;"
        );
        clinit.visitFieldInsn(PUTSTATIC, "jyxal/Main", "register", "Ljava/lang/Object;");

        Compiler compiler = new Compiler(parser, cw, clinit);

        JyxalMethod main = cw.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "main",
                "([Ljava/lang/String;)V"
        );
        compiler.callStack.push(main);
        main.visitCode();

        compiler.visit(parser.file());

        // finish up clinit
        clinit.visitInsn(RETURN);
        clinit.visitEnd();
        clinit.visitMaxs(0, 0);

        // TODO: reverse the signs for the variable assns

        // finish up main
        Label end = new Label();
        main.loadStack();
        main.visitMethodInsn(
                INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "size",
                "()I",
                false
        );
        main.visitJumpInsn(IFEQ, end);

        main.loadStack();
        main.visitMethodInsn(
                INVOKEVIRTUAL,
                "runtime/ProgramStack",
                "pop",
                "()Ljava/lang/Object;",
                false
        );

        main.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        main.visitInsn(SWAP);
        main.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);

        main.visitLabel(end);
        main.visitInsn(RETURN);

        try {
            main.visitEnd();
            main.visitMaxs(0, 0);
        } catch (Exception e) {
            try (OutputStream os = new FileOutputStream("debug.log")) {
                CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), true, new PrintWriter(os));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            throw new RuntimeException(e);
        }

        return cw.toByteArray();
    }

    @Override
    public Void visitInteger(VyxalParser.IntegerContext ctx) {
        JyxalMethod mv = callStack.peek();
        mv.loadStack();
        AsmHelper.addBigComplex(ctx.getText(), mv);
        AsmHelper.push(mv);
        return null;
    }

    @Override
    public Void visitComplex_number(VyxalParser.Complex_numberContext ctx) {
        JyxalMethod mv = callStack.peek();
        String[] parts = COMPLEX_SEPARATOR.split(ctx.getText());
        mv.loadStack();
        AsmHelper.addBigDecimal(parts[0], mv);
        AsmHelper.addBigDecimal(parts[1], mv);
        mv.visitMethodInsn(
                INVOKESTATIC,
                "runtime/math/BigComplex",
                "valueOf",
                "(Ljava/math/BigDecimal;Ljava/math/BigDecimal;)Lruntime/math/BigComplex;",
                false
        );
        AsmHelper.push(mv);

        return null;
    }

    @Override
    public Void visitNormal_string(VyxalParser.Normal_stringContext ctx) {
        JyxalMethod mv = callStack.peek();
        mv.loadStack();
        mv.visitLdcInsn(unescapeJavaString(ctx.any().getText()));
        AsmHelper.push(mv);
        return null;
    }

    @Override
    public Void visitSingle_char_string(VyxalParser.Single_char_stringContext ctx) {
        JyxalMethod mv = callStack.peek();
        mv.loadStack();
        mv.visitLdcInsn(unescapeJavaString(ctx.getText().substring(1)));
        AsmHelper.push(mv);
        return null;
    }

    @Override
    public Void visitDouble_char_string(VyxalParser.Double_char_stringContext ctx) {
        JyxalMethod mv = callStack.peek();
        mv.loadStack();
        mv.visitLdcInsn(unescapeJavaString(ctx.getText().substring(1)));
        AsmHelper.push(mv);
        return null;
    }

    @Override
    public Void visitList(VyxalParser.ListContext ctx) {
        JyxalMethod method = callStack.peek();
        method.visitVarInsn(ALOAD, method.getStackVar());

        AsmHelper.selectNumberInsn(method, ctx.program().size());
        method.visitTypeInsn(ANEWARRAY, "java/lang/Object");

        List<VyxalParser.ProgramContext> program = ctx.program();
        for (int i = 0; i < program.size(); i++) {
            VyxalParser.ProgramContext item = program.get(i);
            method.visitInsn(DUP);
            AsmHelper.selectNumberInsn(method, i);

            if (item.getChildCount() == 1 && item.getChild(0) instanceof VyxalParser.LiteralContext) {
                // we can inline the literal
                visit(item.getChild(0));
                // we need to pop the literal value, it's going to get optimized away anyway
                AsmHelper.pop(method);
                method.visitInsn(SWAP);
                method.visitInsn(POP);
            } else {
                String methodName = "listInit$" + listCounter++;
                JyxalMethod mv = classWriter.visitMethod(
                        ACC_PRIVATE | ACC_STATIC,
                        methodName,
                        "()Ljava/lang/Object;"
                );
                mv.visitCode();
                callStack.push(mv);
                visit(item);
                callStack.pop();

                AsmHelper.pop(mv);
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
            }

            method.visitInsn(AASTORE);
        }

        method.visitMethodInsn(
                INVOKESTATIC,
                "runtime/list/JyxalList",
                "create",
                "([Ljava/lang/Object;)Lruntime/list/JyxalList;",
                false
        );
        AsmHelper.push(method);

        return null;
    }

    @Override
    public Void visitVariable_assn(VyxalParser.Variable_assnContext ctx) {
        String name = ctx.variable().getText();
        if (contextVariables.contains(name)) {
            JyxalMethod mv = callStack.peek();
            if (ctx.ASSN_SIGN().getText().equals("→")) {
                // set
                AsmHelper.pop(mv);
                mv.visitVarInsn(ASTORE, mv.getCtxVar());
            } else {
                // get
                mv.loadStack();
                mv.visitVarInsn(ALOAD, mv.getCtxVar());
                AsmHelper.push(mv);
            }
        } else {
            if (!variables.contains(name)) {
                variables.add(name);

                classWriter.visitField(
                        ACC_PRIVATE | ACC_STATIC,
                        name,
                        "Ljava/lang/Object;",
                        null,
                        null
                );

                clinit.visitFieldInsn(
                        GETSTATIC,
                        "runtime/math/BigComplex",
                        "ZERO",
                        "Lruntime/math/BigComplex;"
                );
                clinit.visitFieldInsn(
                        PUTSTATIC,
                        "jyxal/Main",
                        name,
                        "Ljava/lang/Object;"
                );
            }

            JyxalMethod mv = callStack.peek();
            if (ctx.ASSN_SIGN().getText().equals("→")) {
                // set
                AsmHelper.pop(mv);
                mv.visitFieldInsn(
                        PUTSTATIC,
                        "jyxal/Main",
                        name,
                        "Ljava/lang/Object;"
                );
            } else {
                // get
                mv.loadStack();
                mv.loadStack();
                mv.visitFieldInsn(
                        GETSTATIC,
                        "jyxal/Main",
                        name,
                        "Ljava/lang/Object;"
                );
                AsmHelper.push(mv);
            }
        }

        return null;
    }

    @Override
    public Void visitElement(VyxalParser.ElementContext ctx) {
        String element = ctx.element_type().getText();
        if (ctx.PREFIX() != null) {
            element = ctx.PREFIX().getText() + element;
        }

        Consumer<JyxalMethod> consumer = ctx.MODIFIER() == null ? null : visitModifier(ctx.MODIFIER().getText());

        JyxalMethod mv = callStack.peek();
        Element.getByText(element).compile(classWriter, mv);

        if (consumer != null) {
            consumer.accept(mv);
        }

        return null;
    }

    @Override
    public Void visitWhile_loop(VyxalParser.While_loopContext ctx) {
        Label start = new Label();
        Label end = new Label();
        int childIndex = 0;

        JyxalMethod mv = callStack.peek();
        mv.visitLabel(start);
        if (ctx.program().size() > 1) {
            // we have a finite loop
            visit(ctx.program(0));
            childIndex = 1;
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    "runtime/RuntimeHelpers",
                    "truthValue",
                    "(Lruntime/ProgramStack;)Z",
                    false
            );
            mv.visitJumpInsn(IFEQ, end);
        }

        visit(ctx.program(childIndex));
        mv.visitJumpInsn(GOTO, start);
        mv.visitLabel(end);

        return null;
    }

    @Override
    public Void visitFor_loop(VyxalParser.For_loopContext ctx) {
        if (ctx.variable() != null) {
            contextVariables.add(ctx.variable().getText());
        }

        Label start = new Label();
        Label end = new Label();
        JyxalMethod mv = callStack.peek();

        AsmHelper.pop(mv);
        mv.visitMethodInsn(
                INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "forify",
                "(Ljava/lang/Object;)Ljava/util/Iterator;",
                false
        );
        try (ContextualVariable iteratorVar = mv.reserveVar()) {
            iteratorVar.store();
            mv.visitLabel(start);

            iteratorVar.load();
            mv.visitMethodInsn(
                    INVOKEINTERFACE,
                    "java/util/Iterator",
                    "hasNext",
                    "()Z",
                    true
            );
            mv.visitJumpInsn(IFEQ, end);

            iteratorVar.load();
            mv.visitMethodInsn(
                    INVOKEINTERFACE,
                    "java/util/Iterator",
                    "next",
                    "()Ljava/lang/Object;",
                    true
            );
            mv.visitVarInsn(ASTORE, mv.getCtxVar());

            visit(ctx.program());

            mv.visitJumpInsn(GOTO, start);
            mv.visitLabel(end);
        }

        return null;
    }

    @Override
    public Void visitIf_statement(VyxalParser.If_statementContext ctx) {
        JyxalMethod mv = callStack.peek();
        Label end = new Label();

        AsmHelper.pop(mv);
        mv.visitMethodInsn(
                INVOKESTATIC,
                "runtime/RuntimeHelpers",
                "truthValue",
                "(Ljava/lang/Object;)Z",
                false
        );

        mv.visitJumpInsn(IFEQ, end);

        visit(ctx.program(0));

        if (ctx.program().size() > 1) {
            Label elseEnd = new Label();
            mv.visitJumpInsn(GOTO, elseEnd);
            mv.visitLabel(end);

            visit(ctx.program(1));

            mv.visitLabel(elseEnd);
        } else {
            mv.visitLabel(end);
        }

        return null;
    }

    public Consumer<JyxalMethod> visitModifier(String modifier) {
        JyxalMethod mv = callStack.peek();
        // ß
        if ("\u00DF".equals(modifier)) {
            Label end = new Label();

            mv.loadStack();
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    "runtime/RuntimeHelpers",
                    "truthValue",
                    "(Lruntime/ProgramStack;)Z",
                    false
            );
            mv.visitJumpInsn(IFEQ, end);
            return m -> m.visitLabel(end);
        }

        return null;
    }

    @Override
    public Void visitLambda(VyxalParser.LambdaContext ctx) {
        String lambdaName = "lambda$" + lambdaCounter;
        JyxalMethod mv = classWriter.visitMethod(
                ACC_PRIVATE | ACC_STATIC,
                lambdaName,
                "(Lruntime/ProgramStack;)Ljava/lang/Object;"
        );

        callStack.push(mv);
        visit(ctx.program());
        callStack.pop();

        AsmHelper.pop(mv);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        mv = callStack.peek();

        // λ
        if ("\u03BB".equals(ctx.LAMBDA_TYPE().getText())) {
            mv.loadStack();

            mv.visitTypeInsn(NEW, "runtime/Lambda");
            mv.visitInsn(DUP);

            AsmHelper.selectNumberInsn(mv, ctx.integer() == null ? 1 : Integer.parseInt(ctx.integer().getText()));
            mv.visitLdcInsn(new Handle(
                    H_INVOKESTATIC,
                    "jyxal/Main",
                    lambdaName,
                    "(Lruntime/ProgramStack;)Ljava/lang/Object;",
                    false
            ));
            mv.visitMethodInsn(
                    INVOKESPECIAL,
                    "runtime/Lambda",
                    "<init>",
                    "(ILjava/lang/invoke/MethodHandle;)V",
                    false
            );
            AsmHelper.push(mv);
        }

        lambdaCounter++;

        return null;
    }


    /**
     * Unescapes a string that contains standard Java escape sequences.
     * <ul>
     * <li><strong>&#92;b &#92;f &#92;n &#92;r &#92;t &#92;" &#92;'</strong> :
     * BS, FF, NL, CR, TAB, double and single quote.</li>
     * <li><strong>&#92;X &#92;XX &#92;XXX</strong> : Octal character
     * specification (0 - 377, 0x00 - 0xFF).</li>
     * <li><strong>&#92;uXXXX</strong> : Hexadecimal based Unicode character.</li>
     * </ul>
     *
     * Authored by uklimaschewski, placed in the public domain.
     *
     * @param st
     *            A string optionally containing standard java escape sequences.
     * @return The translated string.
     */
    private static String unescapeJavaString(String st) {

        StringBuilder sb = new StringBuilder(st.length());

        for (int i = 0; i < st.length(); i++) {
            char ch = st.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == st.length() - 1) ? '\\' : st
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                            && st.charAt(i + 1) <= '7') {
                        code += st.charAt(i + 1);
                        i++;
                        if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                                && st.charAt(i + 1) <= '7') {
                            code += st.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\':
                        ch = '\\';
                        break;
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case '\"':
                        ch = '\"';
                        break;
                    case '\'':
                        ch = '\'';
                        break;
                    // Hex Unicode: u????
                    case 'u':
                        if (i >= st.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + st.charAt(i + 2) + st.charAt(i + 3)
                                        + st.charAt(i + 4) + st.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
