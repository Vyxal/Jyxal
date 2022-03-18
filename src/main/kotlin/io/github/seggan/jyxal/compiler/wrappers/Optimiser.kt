package io.github.seggan.jyxal.compiler.wrappers

import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

object Optimiser {

    fun optimise(codeBlock: InsnList, jyxalMethod: JyxalMethod) {
        // first remove all stack loads
        val toRemove = HashSet<AbstractInsnNode>()
        for (insn in codeBlock) {
            if (insn is VarInsnNode && insn.`var` == jyxalMethod.stackVar && insn.opcode == ALOAD) {
                toRemove.add(insn)
                val next = insn.next
                if (next != null && next.opcode == SWAP) {
                    toRemove.add(next)
                }
            }
        }
        for (insn in toRemove) {
            codeBlock.remove(insn)
        }
        toRemove.clear()
        toRemove.clear()
        // then remove all push-pop pairs
        for (insn in codeBlock) {
            if (insn.opcode == INVOKEVIRTUAL
                    && insn is MethodInsnNode
                    && insn.name == "push"
                    && insn.desc == "(Ljava/lang/Object;)V"
                    && insn.owner == "io/github/seggan/jyxal/runtime/ProgramStack") {
                val next = insn.getNext()
                if (next.opcode == INVOKEVIRTUAL
                        && next is MethodInsnNode
                        && next.name == "pop"
                        && next.desc == "()Ljava/lang/Object;"
                        && next.owner == "io/github/seggan/jyxal/runtime/ProgramStack") {
                    toRemove.add(insn)
                    toRemove.add(next)
                }
            }
        }
        for (insn in toRemove) {
            codeBlock.remove(insn)
        }
        for (insn in codeBlock) {
            if (insn is MethodInsnNode) {
                if (insn.owner == "io/github/seggan/jyxal/runtime/ProgramStack" && insn.name != "<init>") {
                    val arguments = (Type.getArgumentsAndReturnSizes(insn.desc) shr 2) - 1
                    codeBlock.insertBefore(insn, VarInsnNode(ALOAD, jyxalMethod.stackVar))
                    if (arguments != 0) {
                        // one argument
                        codeBlock.insertBefore(insn, InsnNode(SWAP))
                    }
                } else if (insn.desc.startsWith("(Lio/github/seggan/jyxal/runtime/ProgramStack;)")) {
                    // these all take the stack as input
                    codeBlock.insertBefore(insn, VarInsnNode(ALOAD, jyxalMethod.stackVar))
                }
            }
        }
    }
}