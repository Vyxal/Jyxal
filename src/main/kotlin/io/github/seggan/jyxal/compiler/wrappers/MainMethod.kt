package io.github.seggan.jyxal.compiler.wrappers

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

class MainMethod internal constructor(cw: ClassWriter, access: Int, name: String, desc: String) : JyxalMethod(cw, access, name, desc) {

    private val start = Label()
    private val end = Label()

    override fun visitEnd() {
        visitLabel(end)
        super.visitEnd()
    }

    init {
        stackVar = 1
        ctxVar = 2
        visitTypeInsn(Opcodes.NEW, "runtime/ProgramStack")
        visitInsn(Opcodes.DUP)
        visitVarInsn(Opcodes.ALOAD, 0)
        visitMethodInsn(Opcodes.INVOKESPECIAL, "runtime/ProgramStack", "<init>", "([Ljava/lang/String;)V", false)
        visitVarInsn(Opcodes.ASTORE, stackVar)
        visitFieldInsn(
            Opcodes.GETSTATIC,
            "runtime/math/BigComplex",
            "ZERO",
            "Lruntime/math/BigComplex;"
        )
        visitVarInsn(Opcodes.ASTORE, ctxVar)
        visitLabel(start)
        visitLocalVariable("stack", "Lio/github/seggan/jyxal/runtime/ProgramStack;", null, start, end, stackVar)
        visitLocalVariable("ctx", "Ljava/lang/Object;", null, start, end, ctxVar)
    }
}