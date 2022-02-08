package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.antlr.VyxalBaseVisitor;
import io.github.seggan.jyxal.antlr.VyxalParser;
import io.github.seggan.jyxal.antlr.VyxalVisitor;
import org.antlr.v4.runtime.tree.ParseTree;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class Compiler implements Opcodes {

    private static final Pattern COMPLEX_SEPARATOR = Pattern.compile("°");

    final VyxalParser parser;
    private final String fileName;

    // must be LinkedHashSet to preserve order
    final Set<String> variables = new LinkedHashSet<>();
    private final Set<String> contextVariables = new HashSet<>();

    private int listCounter = 0;

    public Compiler(VyxalParser parser, String fileName) {
        this.parser = parser;
        this.fileName = fileName;

        VyxalVisitor<Void> visitor = new VyxalBaseVisitor<>() {
            @Override
            public Void visitVariable_assn(VyxalParser.Variable_assnContext ctx) {
                if (ctx.parent.getRuleIndex() == VyxalParser.RULE_for_loop) {
                    contextVariables.add(ctx.variable().getText());
                } else {
                    variables.add(ctx.variable().getText());
                }
                return null;
            }
        };
        visitor.visit(parser.program());
    }

    public byte[] compile() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC + ACC_FINAL, "jyxal/Main", null, "java/lang/Object", null);
        cw.visitSource(fileName, null);

        MethodVisitor main = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        main.visitCode();
        main.visitInsn(ACONST_NULL);

        visitProgram(parser.program(), cw, main);

        Label end = new Label();
        main.visitInsn(DUP);
        main.visitJumpInsn(IFNULL, end);

        main.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        main.visitInsn(SWAP);
        main.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);

        main.visitLabel(end);
        main.visitInsn(RETURN);

        main.visitMaxs(-1, -1); // auto-calculate stack size and number of locals

        return cw.toByteArray();
    }

    private void visitProgram(VyxalParser.ProgramContext ctx, ClassWriter cw, MethodVisitor mv) {
        for (ParseTree child : ctx.children) {
            if (child instanceof VyxalParser.LiteralContext literalContext) {
                ParseTree literal = literalContext.getChild(0);
                if (literal instanceof VyxalParser.StringContext stringContext) {
                    visitString(stringContext, mv);
                } else if (literal instanceof VyxalParser.NumberContext numberContext) {
                    visitNumber(numberContext, mv);
                } else if (literal instanceof VyxalParser.ListContext listContext) {
                    visitList(listContext, cw, mv);
                } else {
                    throw new JyxalCompileException("Unknown literal type: " + literal.getClass().getSimpleName());
                }
            }
        }
    }

    private void visitList(VyxalParser.ListContext listContext, ClassWriter cw, MethodVisitor mv) {
        for (VyxalParser.ProgramContext item : listContext.program()) {
            String methodName = "listInit$" + listCounter++;
            MethodVisitor mvList = cw.visitMethod(
                ACC_PRIVATE | ACC_STATIC,
                methodName,
                "(Ljava/lang/Object;L)V",
                null,
                null
            );
        }
    }

    private void visitNumber(VyxalParser.NumberContext numberContext, MethodVisitor mv) {
        ParseTree child = numberContext.getChild(0);
        if (child instanceof VyxalParser.IntegerContext || child instanceof VyxalParser.DecimalContext) {
            AsmHelper.addBigDecimal(numberContext.getText(), mv);
            mv.visitMethodInsn(INVOKESTATIC, "runtime/math/BigComplex", "valueOf", "(Ljava/math/BigDecimal;)Lruntime/math/BigComplex;", false);
        } else if (child instanceof VyxalParser.ComplexContext complexContext) {
            String[] parts = COMPLEX_SEPARATOR.split(complexContext.getText());
            if (parts.length != 2) {
                throw new JyxalCompileException("Invalid complex number: " + complexContext.getText());
            }
            AsmHelper.addBigDecimal(parts[0], mv);
            AsmHelper.addBigDecimal(parts[1], mv);
            mv.visitMethodInsn(INVOKESTATIC, "runtime/math/BigComplex", "valueOf", "(Ljava/math/BigDecimal;Ljava/math/BigDecimal;)Lruntime/math/BigComplex;", false);
        } else {
            // compressed number
            String number = ((VyxalParser.Compressed_numberContext) child).any_text().getText();
            // TODO: implement
        }
    }

    private void visitString(VyxalParser.StringContext stringContext, MethodVisitor mv) {
        ParseTree child = stringContext.getChild(0);
        if (child instanceof VyxalParser.Normal_stringContext normalStringContext) {
            mv.visitLdcInsn(normalStringContext.any_text().getText());
        } else if (child instanceof VyxalParser.Single_char_stringContext
            || child instanceof VyxalParser.Double_char_stringContext) {
            String s = stringContext.getText().substring(1);
            mv.visitLdcInsn(s);
        } else {
            // compressed string
            String s = ((VyxalParser.Compressed_stringContext) child).any_text().getText();
            // TODO: implement
        }
    }
}
