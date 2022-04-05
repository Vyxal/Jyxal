package io.github.seggan.jyxal.runtime.list

import io.github.seggan.jyxal.runtime.math.BigComplex

internal class LazyList(private val generator: Iterator<Any>) : JyxalList() {

    private val backing: MutableList<Any> = ArrayList()

    private var resolved: Boolean = false

    override val size: Int
        get() {
            return if (resolved) {
                backing.size
            } else {
                var size = 0
                for (i in this) {
                    size++
                }
                resolved = true
                size
            }
        }

    override fun contains(element: Any): Boolean {
        if (backing.contains(element)) {
            return true
        }
        while (generator.hasNext()) {
            val next = generator.next()
            backing.add(next)
            if (next == element) {
                return true
            }
        }
        resolved = true
        return false
    }

    override fun get(ind: Int): Any {
        fill(ind)
        return if (backing.size > ind) {
            backing[ind]
        } else {
            BigComplex.ZERO
        }
    }

    override fun hasInd(ind: Int): Boolean {
        fill(ind)
        return backing.size > ind
    }

    override fun isLazy(): Boolean {
        return true
    }

    override fun toNonLazy(): JyxalList {
        val newList = ArrayList<Any>()
        for (elm in this) {
            newList.add(elm)
        }
        return FiniteList(newList)
    }

    override fun listIterator(): ListIterator<Any> {
        return object : ListIterator<Any> {

            private var ind = 0

            override fun hasNext(): Boolean = hasInd(ind)

            override fun hasPrevious(): Boolean = hasInd(ind - 1)

            override fun next(): Any = get(ind++)

            override fun previous(): Any = get(--ind)

            override fun nextIndex(): Int = ind

            override fun previousIndex(): Int = ind - 1
        }
    }

    override fun toString(): String {
        return "A lazy list, including " + vyxalListFormat(backing)
    }

    private fun fill(index: Int) {
        if (!resolved) {
            while (backing.size <= index) {
                if (!generator.hasNext()) {
                    resolved = true
                    break
                }
                backing.add(generator.next())
            }
        }
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }
}