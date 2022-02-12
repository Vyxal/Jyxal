package io.github.seggan.jyxal.compiler.optimization;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ClassOptimizer extends ClassVisitor {

    public ClassOptimizer(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
        this.cv = classVisitor;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, descriptor, signature, exceptions);
        return new PushPopOptimizer(access, name, descriptor, signature, exceptions, mv);
    }
}
