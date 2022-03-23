package io.github.seggan.jyxal.runtime

import io.github.seggan.jyxal.runtime.list.JyxalList
import io.github.seggan.jyxal.runtime.math.BigComplex
import java.math.BigDecimal
import java.math.BigInteger

fun List<Any>.jyxal(): JyxalList = JyxalList.fromIterableLazy(this)
@JvmName("nullableJyxal")
fun List<Any?>.jyxal(): JyxalList {
    val result = ArrayList<Any>()
    for (item in this) {
        result.add(item!!)
    }
    return JyxalList.fromIterableLazy(result)
}
fun Boolean.jyxal(): BigComplex = if (this) BigComplex.ONE else BigComplex.ZERO
fun Int.jyxal(): BigComplex = BigComplex.valueOf(this.toLong())
fun Long.jyxal(): BigComplex = BigComplex.valueOf(this)
fun BigInteger.jyxal(): BigComplex = BigComplex.valueOf(this.toBigDecimal())
fun BigDecimal.jyxal(): BigComplex = BigComplex.valueOf(this)