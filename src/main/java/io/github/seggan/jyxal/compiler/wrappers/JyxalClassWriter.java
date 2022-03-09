package io.github.seggan.jyxal.compiler.wrappers;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

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

    public JyxalMethod visitMethod(int access, String name, String desc) {
        if (name.equals("main") && desc.equals("([Ljava/lang/String;)V")
                && access == (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) {
            return new MainMethod(this, access, name, desc);
        } else {
            return new Function(this, access, name, desc);
        }
    }
}