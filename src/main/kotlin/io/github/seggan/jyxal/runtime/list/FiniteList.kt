package io.github.seggan.jyxal.runtime.list

import io.github.seggan.jyxal.runtime.math.BigComplex
import java.math.BigInteger

internal class FiniteList : JyxalList {

    private var backing: MutableList<Any>

    override val size: Int
        get() = backing.size

    override fun contains(element: Any): Boolean {
        return backing.contains(element)
    }

    constructor(elements: Collection<Any>) : super() {
        backing = elements.toMutableList()
    }

    constructor() : super() {
        backing = ArrayList()
    }

    override fun get(ind: Int): Any {
        return if (backing.size > ind) {
            backing[ind]
        } else {
            BigComplex.ZERO
        }
    }

    override fun add(element: Any) {
        backing.add(element)
    }

    override fun toNonLazy(): JyxalList {
        return this
    }

    override fun addNew(ind: BigInteger, value: Any): JyxalList {
        val newBacking = mutableListOf<Any>()
        newBacking.add(ind.toInt(), value)
        return FiniteList(newBacking)
    }

    override fun addAllNew(list: JyxalList): JyxalList {
        val newBacking: MutableList<Any> = ArrayList(backing)
        newBacking.addAll(list)
        return FiniteList(newBacking)
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }

    override fun append(value: Any): JyxalList {
        val newBacking: MutableList<Any> = ArrayList(backing)
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
        val newBacking = mutableListOf<Any>()
        for (o in backing) {
            newBacking.add(f(o))
        }
        return FiniteList(newBacking)
    }

    override fun filter(pred: (Any) -> Boolean): JyxalList {
        val newBacking = mutableListOf<Any>()
        for (o in backing) {
            if (pred(o)) {
                newBacking.add(o)
            }
        }
        return FiniteList(newBacking)
    }
}