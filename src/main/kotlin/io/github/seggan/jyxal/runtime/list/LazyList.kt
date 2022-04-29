package io.github.seggan.jyxal.runtime.list

import io.github.seggan.jyxal.runtime.math.BigComplex
import java.math.BigInteger

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

    override fun get(ind: IntProgression): JyxalList {
        val it = this.iterator()
        for (i in 0 until ind.first) {
            it.next()
        }
        return create(object : Iterator<Any> {

            private var index = ind.first

            override fun hasNext(): Boolean {
                return it.hasNext() && index < ind.last
            }

            override fun next(): Any {
                index++
                while (index !in ind) {
                    index++
                    it.next()
                }
                return it.next()
            }
        })
    }

    override fun hasAtLeast(amount: Int): Boolean {
        fill(amount)
        return backing.size >= amount;
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

    override fun remove(ind: Int): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {
            var current = 0

            override fun hasNext(): Boolean {
                return it.hasNext()
            }

            override fun next(): Any {
                if (current == ind) {
                    current++
                    return it.next()
                }
                current++
                return it.next()
            }
        })
    }

    override fun map(f: (Any) -> Any): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {
            override fun hasNext(): Boolean {
                return it.hasNext()
            }

            override fun next(): Any {
                return f(it.next())
            }
        })
    }

    override fun filter(pred: (Any) -> Boolean): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {
            override fun hasNext(): Boolean {
                return it.hasNext()
            }

            override fun next(): Any {
                var next = it.next()
                while (!pred(next)) {
                    if (!it.hasNext()) {
                        throw NoSuchElementException()
                    }
                    next = it.next()
                }
                return next
            }
        })
    }

    override fun add(ind: BigInteger, value: Any): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {
            var current = BigInteger.ZERO

            override fun hasNext(): Boolean {
                return it.hasNext()
            }

            override fun next(): Any {
                if (current == ind) {
                    current = current.add(BigInteger.ONE)
                    return value
                }
                current = current.add(BigInteger.ONE)
                return it.next()
            }
        })
    }

    override fun add(value: Any): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {

            private var last = false

            override fun hasNext(): Boolean {
                return it.hasNext() || last
            }

            override fun next(): Any {
                return if (it.hasNext()) {
                    it.next()
                } else {
                    last = true
                    value
                }
            }
        })
    }

    override fun addAll(iterable: Iterable<Any>): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {

            private val listIt = iterable.iterator()

            override fun hasNext(): Boolean {
                return it.hasNext() || listIt.hasNext()
            }

            override fun next(): Any {
                return if (it.hasNext()) {
                    it.next()
                } else {
                    listIt.next()
                }
            }
        })
    }

    override fun zip(iterable: Iterable<Any>): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {

            private val listIt = iterable.iterator()

            override fun hasNext(): Boolean {
                return it.hasNext() && listIt.hasNext()
            }

            override fun next(): Any {
                return create(it.next(), listIt.next())
            }
        })
    }

    override fun zip(iterable: Iterable<Any>, f: (Any, Any) -> Any): JyxalList {
        val it = this.iterator()
        return LazyList(object : Iterator<Any> {

            private val listIt = iterable.iterator()

            override fun hasNext(): Boolean {
                return it.hasNext() && listIt.hasNext()
            }

            override fun next(): Any {
                return f(it.next(), listIt.next())
            }
        })
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