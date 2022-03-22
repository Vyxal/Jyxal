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

    fun load(opcode: Int) {
        checkClosed()
        if (opcode != Opcodes.ALOAD && opcode != Opcodes.ILOAD && opcode != Opcodes.FLOAD && opcode != Opcodes.DLOAD && opcode != Opcodes.LLOAD) {
            throw IllegalArgumentException("Opcode must be a load opcode, but was $opcode")
        }
        mv.visitVarInsn(opcode, index)
    }

    fun load() = load(Opcodes.ALOAD)

    fun store(opcode: Int) {
        checkClosed()
        if (opcode != Opcodes.ASTORE && opcode != Opcodes.ISTORE && opcode != Opcodes.FSTORE && opcode != Opcodes.DSTORE && opcode != Opcodes.LSTORE) {
            throw IllegalArgumentException("Opcode must be a store opcode, but was $opcode")
        }
        mv.visitVarInsn(opcode, index)
    }

    fun store() = store(Opcodes.ASTORE)

    private fun checkClosed() {
        check(!isClosed) { "Already closed" }
    }
}
