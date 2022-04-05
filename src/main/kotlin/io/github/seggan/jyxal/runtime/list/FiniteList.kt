package io.github.seggan.jyxal.runtime.list

import io.github.seggan.jyxal.runtime.math.BigComplex
import java.math.BigInteger

internal class FiniteList(private val backing: List<Any>) : JyxalList() {

    override val size: Int
        get() = backing.size

    override fun contains(element: Any): Boolean {
        return backing.contains(element)
    }

    constructor() : this(emptyList<Any>())

    override fun get(ind: Int): Any {
        return if (backing.size > ind) {
            backing[ind]
        } else {
            BigComplex.ZERO
        }
    }

    override fun toNonLazy(): JyxalList {
        return this
    }

    override fun add(ind: BigInteger, value: Any): JyxalList {
        val newBacking = ArrayList<Any>(backing)
        newBacking.add(ind.toInt(), value)
        return FiniteList(newBacking)
    }

    override fun addAll(iterable: Iterable<Any>): JyxalList {
        val newBacking = ArrayList(backing)
        newBacking.addAll(iterable)
        return FiniteList(newBacking)
    }

    override fun remove(ind: Int): JyxalList {
        val newBacking = ArrayList(backing)
        newBacking.removeAt(ind)
        return FiniteList(newBacking)
    }

    override fun containsAll(elements: Collection<Any>): Boolean {
        return backing.containsAll(elements)
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }

    override fun add(value: Any): JyxalList {
        val newBacking = ArrayList(backing)
        newBacking.add(value)
        return FiniteList(newBacking)
    }

    override fun listIterator(): ListIterator<Any> {
        return backing.listIterator()
    }

    override fun isLazy(): Boolean {
        return false
    }

    override fun toString(): String {
        return vyxalListFormat(backing)
    }

    override fun hasInd(ind: Int): Boolean {
        return size > ind
    }

    override fun map(f: (Any) -> Any): JyxalList {
        val newBacking = ArrayList<Any>()
        for (o in backing) {
            newBacking.add(f(o))
        }
        return FiniteList(newBacking)
    }

    override fun filter(pred: (Any) -> Boolean): JyxalList {
        val newBacking = ArrayList<Any>()
        for (o in backing) {
            if (pred(o)) {
                newBacking.add(o)
            }
        }
        return FiniteList(newBacking)
    }

    override fun zip(iterable: Iterable<Any>): JyxalList {
        val newBacking = ArrayList<Any>()
        val iter = this.iterator()
        val iter2 = iterable.iterator()
        while (iter.hasNext() && iter2.hasNext()) {
            newBacking.add(create(iter.next(), iter2.next()))
        }
        return FiniteList(newBacking)
    }
}