package io.github.seggan.jyxal.compiler.wrappers

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.INVOKESTATIC

class Function internal constructor(cw: ClassWriter, access: Int, name: String, desc: String) : JyxalMethod(cw, access, name, desc) {
    init {
        stackVar = 0
        ctxVar = 1
        loadStack()
        visitMethodInsn(
            INVOKESTATIC,
            "runtime/list/JyxalList",
            "create",
            "(Ljava/util/Collection;)Lruntime/list/JyxalList;",
            false
        )
        visitVarInsn(ASTORE, ctxVar)
    }
}