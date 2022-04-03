package io.github.seggan.jyxal.runtime

import io.github.seggan.jyxal.runtime.list.JyxalList
import io.github.seggan.jyxal.runtime.math.BigComplex
import java.math.BigDecimal
import java.math.BigInteger

fun List<Any>.jyxal(): JyxalList = JyxalList.create(this)

@JvmName("nullableJyxal")
fun List<Any?>.jyxal(): JyxalList {
    val result = ArrayList<Any>()
    for (item in this) {
        result.add(item!!)
    }
    return JyxalList.create(this)
}

fun Iterable<Any>.jyxal(): JyxalList = JyxalList.fromIterableLazy(this)
fun Boolean.jyxal(): BigComplex = if (this) BigComplex.ONE else BigComplex.ZERO
fun Int.jyxal(): BigComplex = BigComplex.valueOf(this.toLong())
fun Long.jyxal(): BigComplex = BigComplex.valueOf(this)
fun BigInteger.jyxal(): BigComplex = BigComplex.valueOf(this.toBigDecimal())
fun BigDecimal.jyxal(): BigComplex = BigComplex.valueOf(this)

operator fun <T> List<T>.times(n: Int): List<T> {
    val result = ArrayList<T>()
    for (i in 0 until n) {
        result.addAll(this)
    }
    return result
}

@JvmName("mutableTimes")
operator fun <T> MutableList<T>.times(n: Int): MutableList<T> {
    val result = ArrayList<T>()
    for (i in 0 until n) {
        result.addAll(this)
    }
    return result
}

operator fun CharSequence.times(n: Int): String {
    val result = StringBuilder()
    for (i in 0 until n) {
        result.append(this)
    }
    return result.toString()
}

operator fun String.times(n: Int): String = JavaNatives.repeat(this, n)