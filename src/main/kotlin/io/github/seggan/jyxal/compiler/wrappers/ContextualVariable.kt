package io.github.seggan.jyxal.compiler.wrappers

import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes

data class ContextualVariable(internal val index: Int, private val mv: JyxalMethod) : AutoCloseable {

    internal val end = Label()

    private var isClosed = false

    override fun close() {
        checkClosed()
        mv.freeVar(this)
        isClosed = true
    }

    fun load() {
        checkClosed()
        mv.visitVarInsn(Opcodes.ALOAD, index)
    }

    fun store() {
        checkClosed()
        mv.visitVarInsn(Opcodes.ASTORE, index)
    }

    private fun checkClosed() {
        check(!isClosed) { "Already closed" }
    }
}
