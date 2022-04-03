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

    override fun hashCode(): Int {
        return backing.hashCode()
    }

    override fun add(value: Any): JyxalList {
        val newBacking = ArrayList(backing)
        newBacking.add(value)
        return FiniteList(newBacking)
    }

    override fun iterator(): Iterator<Any> {
        return backing.iterator()
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
}