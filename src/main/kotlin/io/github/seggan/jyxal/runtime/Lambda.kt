package io.github.seggan.jyxal.runtime

import java.lang.invoke.MethodHandle

data class Lambda(val arity: Int, val handle: MethodHandle) {

    fun call(stack: ProgramStack): Any {
        val args: MutableList<Any> = ArrayList()
        for (i in 0 until arity) {
            args.add(stack.pop())
        }
        return try {
            handle.invoke(ProgramStack(args))
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    fun call(arg: Any?): Any {
        if (arity != 1) {
            throw RuntimeException("Invalid arity")
        }
        return try {
            handle.invoke(ProgramStack(arg))
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }
}