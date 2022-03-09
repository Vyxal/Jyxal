package io.github.seggan.jyxal.compiler.wrappers;

import org.objectweb.asm.ClassWriter;

public class Function extends JyxalMethod {

    Function(ClassWriter cw, int access, String name, String desc) {
        super(cw, access, name, desc);

        stackVar = 0;
        ctxVar = 1;

        loadStack();
        visitMethodInsn(
                INVOKESTATIC,
                "runtime/list/JyxalList",
                "create",
                "(Ljava/util/Collection;)Lruntime/list/JyxalList;",
                false
        );
        visitVarInsn(ASTORE, ctxVar);
    }
}
