package io.github.seggan.jyxal.runtime.list

import io.github.seggan.jyxal.runtime.ProgramStack
import io.github.seggan.jyxal.runtime.jyxal
import io.github.seggan.jyxal.runtime.math.BigComplex
import io.github.seggan.jyxal.runtime.plus
import java.math.BigInteger

abstract class JyxalList : Collection<Any> {

    companion object {

        fun create(generator: Iterator<Any>): JyxalList {
            return LazyList(generator)
        }

        fun create(vararg array: Any): JyxalList {
            return FiniteList(listOf(*array))
        }

        fun create(collection: Collection<Any>): JyxalList {
            return FiniteList(collection.toList())
        }

        fun create(list: List<Any>): JyxalList {
            return FiniteList(ArrayList(list))
        }

        @JvmStatic
        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        fun create(stack: ProgramStack): JyxalList {
            val list = ArrayList<Any>(stack.size)
            while (!stack.isEmpty()) {
                list.add(stack.removeLast()!!)
            }
            return FiniteList(list)
        }

        fun create(): JyxalList {
            return FiniteList()
        }

        /**
         * Create an infinite list
         */
        fun createInf(generator: () -> Any): JyxalList {
            return LazyList(object : Iterator<Any> {
                override fun hasNext(): Boolean {
                    return true
                }

                override fun next(): Any {
                    return generator()
                }
            })
        }

        fun range(start: BigComplex, end: BigComplex): JyxalList {
            return LazyList(object : Iterator<BigComplex> {
                var current = start

                override fun hasNext(): Boolean {
                    return current < end
                }

                override fun next(): BigComplex {
                    val result = current
                    current += 1
                    return result
                }
            })
        }

        fun range(start: Int, end: Int): JyxalList {
            return LazyList(object : Iterator<BigComplex> {
                var current = start

                override fun hasNext(): Boolean {
                    return current < end
                }

                override fun next(): BigComplex {
                    val result = current++
                    return result.jyxal()
                }
            })
        }

        fun fromIterableLazy(iterable: Iterable<Any>): JyxalList {
            return LazyList(iterable.iterator())
        }
    }

    protected fun vyxalListFormat(list: List<Any>): String {
        val sb = StringBuilder()
        sb.append("⟨")
        val it = list.iterator()
        while (true) {
            if (!it.hasNext()) {
                if (sb.length > 1) {
                    sb.delete(sb.length - 3, sb.length)
                }
                return sb.append("⟩").toString()
            }
            sb.append(it.next())
            sb.append(" | ")
        }
    }

    /**
     * Whether there exists an element at the given index
     */
    abstract fun hasInd(ind: Int): Boolean

    abstract fun isLazy(): Boolean

    abstract fun toNonLazy(): JyxalList

    abstract operator fun get(ind: Int): Any
    abstract operator fun get(ind: IntProgression): JyxalList

    abstract fun hasAtLeast(amount: Int): Boolean

    abstract fun listIterator(): ListIterator<Any>

    override fun iterator(): Iterator<Any> {
        return listIterator()
    }

    abstract fun remove(ind: Int): JyxalList

    abstract fun map(f: (Any) -> Any): JyxalList

    abstract fun filter(pred: (Any) -> Boolean): JyxalList

    abstract fun add(ind: BigInteger, value: Any): JyxalList

    abstract fun add(value: Any): JyxalList

    abstract fun addAll(iterable: Iterable<Any>): JyxalList

    abstract fun zip(iterable: Iterable<Any>): JyxalList

    abstract fun zip(iterable: Iterable<Any>, f: (Any, Any) -> Any): JyxalList

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JyxalList) return false

        val it1 = this.iterator()
        val it2 = other.iterator()
        while (it1.hasNext() && it2.hasNext()) {
            if (it1.next() != it2.next()) {
                return false
            }
        }

        return !it1.hasNext() && !it2.hasNext()
    }

    abstract override fun hashCode(): Int

    override fun containsAll(elements: Collection<Any>): Boolean {
        for (element in elements) {
            if (!this.contains(element)) {
                return false
            }
        }
        return true
    }

    override fun isEmpty(): Boolean {
        return !this.iterator().hasNext()
    }
}
