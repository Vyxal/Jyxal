package io.github.seggan.jyxal.runtime

import java.lang.invoke.MethodHandle

data class Lambda(val arity: Int, val handle: MethodHandle) {

    fun call(stack: ProgramStack): Any {
        val args: MutableList<Any> = ArrayList()
        for (i in 0 until arity) {
            args.add(stack.pop())
        }
        return handle.invoke(ProgramStack(args))
    }

    fun call(arg: Any): Any {
        if (arity != 1) {
            throw RuntimeException("Invalid arity")
        }
        return handle.invoke(ProgramStack(arg))
    }

    fun call(vararg args: Any): Any {
        if (arity != args.size) {
            throw RuntimeException("Invalid arity")
        }
        return handle.invoke(ProgramStack(args))
    }
}