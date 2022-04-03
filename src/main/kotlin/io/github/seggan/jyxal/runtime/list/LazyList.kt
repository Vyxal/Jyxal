package io.github.seggan.jyxal.runtime.list

import io.github.seggan.jyxal.runtime.math.BigComplex

internal class LazyList(private val generator: Iterator<Any>) : JyxalList() {

    private var backing: MutableList<Any> = ArrayList()

    override val size: Int = -1

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

    override fun add(element: Any) {
        throw UnsupportedOperationException("Cannot plus to the end of an infinite list")
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

    override fun iterator(): Iterator<Any> {
        return object : Iterator<Any> {
            private var ind = 0
            override fun hasNext(): Boolean {
                return hasInd(ind)
            }

            override fun next(): Any {
                val elem = this@LazyList[ind]
                ind++
                return elem
            }
        }
    }

    override fun toString(): String {
        return "A lazy list, including " + vyxalListFormat(backing)
    }

    private fun fill(index: Int) {
        while (backing.size <= index && generator.hasNext()) {
            backing.add(generator.next())
        }
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }
}