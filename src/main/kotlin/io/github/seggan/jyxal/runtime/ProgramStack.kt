package io.github.seggan.jyxal.runtime

import io.github.seggan.jyxal.runtime.math.BigComplex
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Deque

class ProgramStack : ArrayDeque<Any?>, Deque<Any?> {

    private var input: Array<Any>?
    private var flags: String? = null
    private var index = 0

    constructor() : super() {
        input = null
        flags = null
    }

    // the String[] is the program args
    constructor(strings: Array<String>) : super() {
        if (strings.isNotEmpty()) {
            flags = strings[0]
            if (strings.size > 1) {
                if (flags!!.indexOf('f') != -1) {
                    input = arrayOf(1)
                    input!![0] = Files.readString(Path.of(strings[1]))
                } else {
                    input = arrayOf(strings.size - 1)
                    for (i in 1 until strings.size) {
                        input!![i - 1] = eval(strings[i])
                    }
                }
            }
        }
        input = null
    }

    constructor(vararg objects: Any?) : super(listOf(*objects)) {
        input = arrayOf(objects)
        flags = null
    }

    constructor(c: Collection<Any>) : super(c) {
        input = c.toTypedArray()
        flags = null
    }

    override fun pop(): Any {
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        return if (this.isEmpty()) {
            getInput()
        } else {
            super.pop()!!
        }
    }

    fun swap() {
        val a = pop()
        val b = pop()
        this.push(a)
        this.push(b)
    }

    /**
     * This modifies this stack
     */
    fun reverse(): ProgramStack {
        val reversed = ProgramStack()
        while (!this.isEmpty()) {
            reversed.push(pop())
        }
        reversed.input = input
        reversed.flags = flags
        return reversed
    }

    fun push(b: Boolean) {
        push(BigComplex.valueOf(b))
    }

    fun push(i: Long) {
        push(BigComplex.valueOf(i))
    }

    fun getInput(): Any {
        return if (input == null || input!!.isEmpty()) {
            BigComplex.ZERO
        } else {
            index %= input!!.size
            input!![index++]
        }
    }
}