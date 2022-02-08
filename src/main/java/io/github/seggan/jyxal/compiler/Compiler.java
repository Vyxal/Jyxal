package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.antlr.VyxalBaseVisitor;
import io.github.seggan.jyxal.antlr.VyxalParser;
import io.github.seggan.jyxal.antlr.VyxalVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.LinkedHashSet;
import java.util.Set;

public final class Compiler implements Opcodes {

    final VyxalParser parser;
    private final String fileName;

    // must be LinkedHashSet to preserve order
    final Set<String> variables;

    public Compiler(VyxalParser parser, String fileName) {
        this.parser = parser;
        this.fileName = fileName;
        this.variables = new LinkedHashSet<>();

        VyxalVisitor<Void> visitor = new VyxalBaseVisitor<>() {
            @Override
            public Void visitVariable_assn(VyxalParser.Variable_assnContext ctx) {
                variables.add(ctx.variable().getText());
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

        main.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        main.visitLdcInsn("Hello, ");
        main.visitLdcInsn("World!");
        main.visitMethodInsn(INVOKESTATIC, "runtime/MathMethods", "add", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
        main.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/Object;)V", false);
        main.visitInsn(RETURN);

        main.visitMaxs(-1, -1); // auto-calculate stack size and number of locals

        return cw.toByteArray();
    }
}
