package io.github.seggan.jyxal.compiler;

import org.objectweb.asm.ClassWriter;

import java.util.regex.Pattern;

public class JyxalClassWriter extends ClassWriter {

    private static final Pattern RUNTIME = Pattern.compile("^runtime/");

    public JyxalClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        type1 = RUNTIME.matcher(type1).replaceAll("io/github/seggan/jyxal/runtime/");
        type2 = RUNTIME.matcher(type2).replaceAll("io/github/seggan/jyxal/runtime/");
        return super.getCommonSuperClass(type1, type2);
    }
}