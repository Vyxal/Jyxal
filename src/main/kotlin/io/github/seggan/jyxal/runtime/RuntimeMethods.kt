@file:Suppress("unused", "MemberVisibilityCanBePrivate")
@file:JvmName("RuntimeMethods")

package io.github.seggan.jyxal.runtime

import io.github.seggan.jyxal.runtime.list.JyxalList
import io.github.seggan.jyxal.runtime.math.BigComplex
import io.github.seggan.jyxal.runtime.math.BigComplexMath
import io.github.seggan.jyxal.runtime.text.JsonParser
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.invoke.MethodHandle
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.function.IntPredicate
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import kotlin.math.sqrt

private val COMMA_PATTERN: Pattern by lazy(LazyThreadSafetyMode.NONE) { Pattern.compile(",") }
private val SPACE_PATTERN: Pattern by lazy(LazyThreadSafetyMode.NONE) { Pattern.compile(" ") }
private val PLUS_SPACE_I_PATTERN: Pattern by lazy(LazyThreadSafetyMode.NONE) { Pattern.compile("[+\\si]") }

private val regexCache = mutableMapOf<String, Pattern>()

fun add(stack: ProgramStack): Any {
    val b = stack.pop()
    val a = stack.pop()
    return addImpl(a, b)
}

private fun addImpl(a: Any, b: Any): Any {
    if (a is JyxalList) {
        if (b is JyxalList) {
            return a.zip(b) { o, p -> addImpl(o, p) }.jyxal()
        }
        return a.map { item: Any -> addImpl(item, b) }
    } else if (b is JyxalList) {
        return b.map { item: Any -> addImpl(a, item) }
    }
    return if (a is BigComplex && b is BigComplex) {
        a + b
    } else {
        a.toString() + b.toString()
    }
}

fun all(obj: Any): Any {
    if (obj is JyxalList) {
        for (item in obj) {
            if (!truthValue(item)) {
                return BigComplex.valueOf(false)
            }
        }
        return BigComplex.valueOf(true)
    } else if (obj is String) {
        for (c in obj.toCharArray()) {
            if (!VOWELS.contains(c)) {
                return BigComplex.valueOf(false)
            }
        }
        return BigComplex.valueOf(true)
    }
    return BigComplex.valueOf(true)
}

fun chrOrd(obj: Any): Any {
    return if (obj is BigComplex) {
        Character.toString(obj.re.intValueExact())
    } else {
        val str = obj.toString()
        if (str.length == 1) {
            BigComplex.valueOf(str[0].code.toLong())
        } else {
            val list = JyxalList.create()
            for (c in str.toCharArray()) {
                list.add(c.code)
            }
            list
        }
    }
}

private fun compare(stack: ProgramStack, predicate: IntPredicate): Any {
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex && b is BigComplex) {
        BigComplex.valueOf(predicate.test(a.compareTo(b)))
    } else {
        BigComplex.valueOf(predicate.test(a.toString().compareTo(b.toString())))
    }
}

fun complement(obj: Any): Any {
    return if (obj is BigComplex) {
        BigComplex.ONE - obj
    } else {
        JyxalList.create(COMMA_PATTERN.split(obj.toString()))
    }
}

fun divide(stack: ProgramStack): Any {
    val o = vectorise(2, ::divide, stack)
    if (o != null) return o
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex) {
        if (b is BigComplex) {
            return a.divide(b, MathContext.DECIMAL128)
        }
        val list = JyxalList.create()
        var sb = StringBuilder()
        var count = BigInteger.ZERO
        val max = a.re.toBigInteger()
        for (c in a.toString().toCharArray()) {
            if (count == max) {
                list.add(sb.toString())
                sb = StringBuilder()
            }
            sb.append(c)
            count = count.add(BigInteger.ONE)
        }
        if (sb.isNotEmpty()) {
            list.add(sb.toString())
        }
        list
    } else if (b is BigComplex) {
        val list = JyxalList.create()
        var sb = StringBuilder()
        var count = BigInteger.ZERO
        val max = b.re.toBigInteger()
        for (c in a.toString().toCharArray()) {
            if (count == max) {
                list.add(sb.toString())
                sb = StringBuilder()
            }
            sb.append(c)
            count = count.add(BigInteger.ONE)
        }
        if (sb.isNotEmpty()) {
            list.add(sb.toString())
        }
        list
    } else {
        val list = JyxalList.create()
        val str = a.toString()
        val delimiter = b.toString()
        var start = 0
        var end = str.indexOf(delimiter)
        while (end != -1) {
            list.add(str.substring(start, end))
            start = end + delimiter.length
            end = str.indexOf(delimiter, start)
        }
        if (start < str.length) {
            list.add(str.substring(start))
        }
        list
    }
}

fun doubleRepeat(obj: Any): Any {
    return when (obj) {
        is JyxalList -> {
            obj.map(::doubleRepeat)
        }
        is BigComplex -> {
            obj * 2
        }
        is Lambda -> {
            Lambda(2, obj.handle)
        }
        else -> {
            obj.toString().repeat(2)
        }
    }
}

fun equal(stack: ProgramStack): Any {
    val o = vectorise(2, ::equal, stack)
    if (o != null) return o
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex && b is BigComplex) {
        BigComplex.valueOf(a == b)
    } else {
        BigComplex.valueOf(a.toString() == b.toString())
    }
}

fun flatten(obj: Any): Any {
    return if (obj is JyxalList) {
        flattenImpl(obj)
    } else {
        val list = JyxalList.create()
        for (c in obj.toString().toCharArray()) {
            list.add(c.toString())
        }
        list
    }
}

private fun flattenImpl(list: JyxalList): JyxalList {
    val newList = ArrayList<Any>()
    for (item in list) {
        if (item is JyxalList) {
            newList.addAll(flattenImpl(item))
        } else {
            newList.add(item)
        }
    }
    return newList.jyxal()
}

fun functionCall(stack: ProgramStack): Any {
    return when (val obj = stack.pop()) {
        is Lambda -> {
            obj.call(stack)
        }
        is JyxalList -> {
            obj.map { o: Any -> BigComplex.valueOf(!truthValue(o)) }
        }
        is BigComplex -> {
            primeFactors(obj) { HashSet() }.size
        }
        else -> {
            exec(obj.toString())
        }
    }
}

@Throws(IOException::class)
fun getRequest(obj: Any): Any {
    var url = obj.toString()
    if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "http://$url"
    }
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 Jyxal")
    connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8")
    connection.instanceFollowRedirects = true
    connection.connect()
    val code = connection.responseCode
    if (code / 100 == 3) {
        val location = connection.getHeaderField("Location")
        return if (location != null) {
            getRequest(location)
        } else {
            throw IOException("Redirect without location")
        }
    } else if (code / 100 != 2) {
        return BigComplex.valueOf(code.toLong())
    }
    var response: ByteArray
    connection.inputStream.use { inputStream -> response = inputStream.readAllBytes() }
    if (connection.contentEncoding == "gzip") {
        try {
            GZIPInputStream(ByteArrayInputStream(response)).use { stream -> return String(stream.readAllBytes(), StandardCharsets.UTF_8) }
        } catch (e: IOException) {
            return String(response, StandardCharsets.UTF_8)
        }
    }
    return String(response, StandardCharsets.UTF_8)
}

fun greaterThan(stack: ProgramStack): Any {
    val o = vectorise(2, ::greaterThanOrEqual, stack)
    return o ?: compare(stack) { i: Int -> i > 0 }
}

fun greaterThanOrEqual(stack: ProgramStack): Any {
    val o = vectorise(2, ::greaterThanOrEqual, stack)
    return o ?: compare(stack) { i: Int -> i >= 0 }
}

fun halve(stack: ProgramStack): Any {
    val o = vectorise(1, ::halve, stack)
    if (o != null) return o
    val obj = stack.pop()
    return if (obj is BigComplex) {
        obj.divide(BigComplex.TWO, MathContext.DECIMAL128)
    } else {
        val str = obj.toString()
        val limit = str.length / 2 + 1
        stack.push(str.substring(0, limit))
        str.substring(limit)
    }
}

fun head(obj: Any): Any {
    return if (obj is JyxalList) {
        obj[0]
    } else {
        if (obj.toString().isNotEmpty()) {
            obj.toString().substring(0, 1)
        } else {
            BigComplex.ZERO
        }
    }
}

fun headExtract(stack: ProgramStack): Any {
    val obj = stack.pop()
    return if (obj is JyxalList) {
        stack.push(obj[0])
        val iterator = obj.iterator()
        iterator.next()
        JyxalList.create(iterator)
    } else {
        stack.push(obj.toString().substring(1))
        obj.toString().substring(0, 1)
    }
}

fun increment(obj: Any): Any {
    return if (obj is BigComplex) {
        obj + 1
    } else {
        SPACE_PATTERN.matcher(obj.toString()).replaceAll("0")
    }
}

fun infinitePrimes(): Any {
    return JyxalList.createInf(object : Supplier<Any> {
        var i = BigComplex.ONE

        override fun get(): Any {
            i += 1
            while (isPrime(i) === BigComplex.ZERO) {
                i += 1
            }
            return i
        }
    })
}

fun infiniteReplace(stack: ProgramStack): Any {
    val c = stack.pop()
    val b = stack.pop()
    val a = stack.pop()
    return if (a is JyxalList) {
        var prev: JyxalList = a
        var list: JyxalList = a
        do {
            list = list.map { o: Any -> if (o == b) c else o }
            if (list == prev) break
            prev = list
        } while (true)
        prev
    } else {
        var aString = a.toString()
        val bString = b.toString()
        val cString = c.toString()
        var prev = aString
        do {
            aString = aString.replace(bString, cString)
            if (a == prev) break
            prev = aString
        } while (true)
        aString
    }
}

fun ior(obj: Any): Any {
    return when (obj) {
        is JyxalList -> {
            obj.map(::ior)
        }
        is BigComplex -> {
            JyxalList.range(BigComplex.ONE, obj + 1)
        }
        else -> {
            obj.toString().uppercase()
        }
    }
}

fun isPrime(obj: Any): Any {
    return if (obj is BigComplex) {
        val n = obj.re.toBigInteger()
        if (n < BigInteger.valueOf(Long.MAX_VALUE) && n > BigInteger.valueOf(Long.MIN_VALUE)) {
            val l = n.toLong()
            if (l < 2) return BigComplex.ZERO
            if (l == 2L || l == 3L) return BigComplex.ONE
            if (l and 1 == 0L || l % 3 == 0L) return BigComplex.ZERO
            val sqrt = sqrt(n.toDouble()).toLong() + 1
            var step = 4L
            var i = 6L
            while (i <= sqrt) {
                if (l % i == 0L) {
                    return BigComplex.ZERO
                }
                step = 6 - step
                i += step
            }
        } else {
            // we don't need to check if n is a small prime, because the other branch will do it
            if (!n.testBit(0)) return BigComplex.ZERO
            val sqrt = n.sqrt().add(BigInteger.ONE)
            val six = BigInteger.valueOf(6)
            var i = six
            while (i <= sqrt) {
                if (n % (i - BigInteger.ONE) == BigInteger.ZERO || n % (i + BigInteger.ONE) == BigInteger.ZERO) {
                    return BigComplex.ZERO
                }
                i += six
            }
        }
        BigComplex.ONE
    } else {
        val str = obj.toString()
        val isUppercase = Character.isUpperCase(str[0])
        for (c in str.toCharArray()) {
            if (Character.isUpperCase(c) != isUppercase) {
                return BigComplex.valueOf(-1)
            }
        }
        BigComplex.valueOf(isUppercase)
    }
}

fun itemSplit(stack: ProgramStack): Any {
    var obj = stack.pop()
    if (obj is BigComplex) {
        obj = obj.re.toBigInteger().toString()
    }
    return if (obj is JyxalList) {
        val listSize = obj.size - 1
        for (i in 0 until listSize) {
            val item = obj[i]
            stack.push(item)
        }
        obj[listSize]
    } else {
        val charArray = obj.toString().toCharArray()
        var i = 0
        val charArrayLength = charArray.size - 1
        while (i < charArrayLength) {
            val c = charArray[i]
            stack.push(c.toString())
            i++
        }
        charArray[charArray.size - 1].toString()
    }
}

fun izr(obj: Any): Any {
    return if (obj is BigComplex) {
        JyxalList.range(BigComplex.ZERO, obj + 1)
    } else {
        val list = JyxalList.create()
        for (c in obj.toString().toCharArray()) {
            list.add(BigComplex.valueOf(Character.isAlphabetic(c.code)))
        }
        list
    }
}

fun joinByNothing(obj: Any): Any {
    return when (obj) {
        is JyxalList -> {
            val sb = StringBuilder()
            for (item in obj) {
                sb.append(item)
            }
            sb.toString()
        }
        is BigComplex -> {
            BigComplex.valueOf(obj.abs(MathContext.DECIMAL128) <= BigDecimal.ONE)
        }
        is Lambda -> {
            var result = BigComplex.ZERO
            while (!truthValue(obj.call(result))) {
                result += 1
            }
            result
        }
        else -> {
            obj.toString()
        }
    }
}

fun joinByNewlines(obj: Any): Any {
    val sb = StringBuilder()
    val it = iterator(obj)
    while (it.hasNext()) {
        sb.append(it.next())
        sb.append('\n')
    }
    return sb.toString()
}

fun jsonParse(obj: Any): Any {
    return JsonParser(obj.toString()).parse()
}

fun length(obj: Any): Any {
    return if (obj is JyxalList) {
        if (obj.isLazy()) {
            var length: Long = 0
            for (ignored in obj) {
                length++
            }
            BigInteger.valueOf(length)
        } else {
            BigComplex.valueOf(obj.size.toLong())
        }
    } else {
        BigComplex.valueOf(obj.toString().length.toLong())
    }
}

fun lessThan(stack: ProgramStack): Any {
    val o = vectorise(2, ::lessThan, stack)
    return o ?: compare(stack) { i: Int -> i < 0 }
}

fun lessThanOrEqual(stack: ProgramStack): Any {
    val o = vectorise(2, ::lessThanOrEqual, stack)
    return o ?: compare(stack) { i: Int -> i <= 0 }
}

fun logicalAnd(stack: ProgramStack): Any {
    val b = stack.pop()
    val a = stack.pop()
    return if (truthValue(a)) {
        if (truthValue(b)) {
            a
        } else {
            b
        }
    } else {
        a
    }
}

fun logicalOr(stack: ProgramStack): Any {
    val b = stack.pop()
    val a = stack.pop()
    return if (truthValue(a)) {
        a
    } else {
        if (truthValue(b)) {
            b
        } else {
            a
        }
    }
}

fun mapGetSet(stack: ProgramStack): Any {
    val map = stack.pop()
    var key = stack.pop()
    if (key is JyxalList) {
        val list = key
        // set
        key = stack.pop()
        for ((i, o) in list.withIndex()) {
            if (o is JyxalList) {
                if (o.size >= 2 && o[0] == key) {
                    return JyxalList.create(replacementIterator(
                            list.iterator(),
                            i,
                            JyxalList.create(o[0], map)
                    ))
                }
            }
        }
        val newList = JyxalList.create(list)
        newList.add(JyxalList.create(key, map))
        return newList
    }
    for (o in map as JyxalList) {
        if (o is JyxalList) {
            if (o.size >= 2 && o[0] == key) {
                return if (o.size == 2) {
                    o[1]
                } else {
                    val iterator = o.iterator()
                    iterator.next()
                    JyxalList.create(iterator)
                }
            }
        }
    }
    return BigComplex.ZERO
}

fun merge(stack: ProgramStack): Any {
    val b = stack.pop()
    val a = stack.pop()
    return if (a is JyxalList) {
        if (b is JyxalList) {
            a.addAllNew(b)
        } else {
            a.append(b)
        }
    } else if (b is JyxalList) {
        b.addNew(BigInteger.ZERO, a)
    } else {
        a.toString() + b.toString()
    }
}

fun moduloFormat(stack: ProgramStack): Any {
    val o = vectorise(2, ::multiCommand, stack)
    if (o != null) return o
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex && b is BigComplex) {
        // BigComplex has no mod method...
        if (b.isReal) {
            BigComplex.valueOf(a.re.remainder(b.re), a.im.remainder(b.re))
        } else {
            throw RuntimeException("Can't modulo ((BigComplex) obj) numbers with non-real numbers")
        }
    } else {
        if (a is BigComplex) {
            b.toString().replace("%", a.toString())
        } else {
            a.toString().replace("%", b.toString())
        }
    }
}

fun multiCommand(stack: ProgramStack): Any {
    val o = vectorise(2, ::multiCommand, stack)
    if (o != null) return o
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex) {
        if (b is BigComplex) {
            b loga a
        } else {
            repeatCharacters(b.toString(), a.re.toInt())
        }
    } else if (b is BigComplex) {
        repeatCharacters(a.toString(), b.re.toInt())
    } else {
        val sb = StringBuilder()
        val aString = a.toString()
        val bString = b.toString()
        for (i in aString.indices) {
            val ch = bString[i % bString.length]
            val aChar = aString[i]
            if (Character.isUpperCase(ch)) {
                sb.append(aChar.uppercaseChar())
            } else if (Character.isLowerCase(ch)) {
                sb.append(aChar.lowercaseChar())
            } else {
                sb.append(aChar)
            }
        }
        if (aString.length > bString.length) {
            sb.append(aString.substring(bString.length))
        }
        sb.toString()
    }
}

fun multiply(stack: ProgramStack): Any {
    val o = vectorise(2, ::multiply, stack)
    if (o != null) return o
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex) {
        if (b is BigComplex) {
            return a * b
        } else if (b is Lambda) {
            return Lambda(a.re.toInt(), b.handle)
        }
        b.toString().repeat(a.re.toInt())
    } else if (a is Lambda && b is BigComplex) {
        Lambda(b.re.toInt(), a.handle)
    } else {
        val aString = a.toString()
        if (b is BigComplex) {
            return aString.repeat(b.re.toInt())
        }
        val bString = b.toString()
        val sb = StringBuilder()
        for (c in bString.toCharArray()) {
            val index = aString.indexOf(c)
            if (index >= 0) {
                sb.append(aString[(index + 1) % aString.length])
            } else {
                sb.append(c)
            }
        }
        sb.toString()
    }
}

fun prepend(stack: ProgramStack): Any {
    val b = stack.pop()
    val a = stack.pop()
    return if (a is JyxalList) {
        a.addNew(BigInteger.ZERO, b)
    } else if (a is BigComplex && b is BigComplex) {
        BigComplex.valueOf(BigDecimal(b.toString() + a.toString()))
    } else {
        a.toString() + b.toString()
    }
}

fun printToFile(stack: ProgramStack) {
    FileOutputStream("test.out").use { os ->
        while (!stack.isEmpty()) {
            os.write(stack.pop().toString().toByteArray(StandardCharsets.UTF_8))
        }
    }
}

fun removeAtIndex(stack: ProgramStack): Any {
    val a = stack.pop()
    val b = stack.pop()
    return if (a is BigComplex) {
        if (b is JyxalList) {
            return b.removeAtIndex(a.re.toBigInteger())
        }
        val str = b.toString()
        val index = a.re.toInt()
        val sb = StringBuilder()
        for (i in str.indices) {
            if (i != index) {
                sb.append(str[i])
            }
        }
        sb.toString()
    } else if (b is BigComplex) {
        if (a is JyxalList) {
            return a.removeAtIndex(b.re.toBigInteger())
        }
        val str = a.toString()
        val index = b.re.toInt()
        val sb = StringBuilder()
        for (i in str.indices) {
            if (i != index) {
                sb.append(str[i])
            }
        }
        sb.toString()
    } else {
        throw IllegalArgumentException("$a, $b")
    }
}

fun reverse(obj: Any): Any {
    return if (obj is JyxalList) {
        val newList = ArrayList<Any>()
        for (item in obj) {
            newList.add(0, item)
        }
        JyxalList.create(newList)
    } else {
        val str = obj.toString()
        val sb = StringBuilder()
        for (i in str.length - 1 downTo 0) {
            sb.append(str[i])
        }
        sb.toString()
    }
}

fun sliceUntil(stack: ProgramStack): Any {
    val o = vectorise(2, ::sliceUntil, stack)
    if (o != null) return o
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex) {
        sliceUntilImpl(b, a.re.toBigInteger())
    } else if (b is BigComplex) {
        sliceUntilImpl(a, b.re.toBigInteger())
    } else {
        val matcher = regexCache.computeIfAbsent(a.toString(), Pattern::compile).matcher(b.toString())
        val list = JyxalList.create()
        while (matcher.find()) {
            list.add(matcher.group())
        }
        list
    }
}

private fun sliceUntilImpl(a: Any, b: BigInteger): Any {
    val iterator = iterator(a)
    return JyxalList.create(object : Iterator<Any> {
        private var count = BigInteger.ZERO

        override fun hasNext(): Boolean {
            return count < b
        }

        override fun next(): Any {
            count += BigInteger.ONE
            return iterator.next()
        }
    })
}

fun sortByFunction(stack: ProgramStack): Any {
    val b = stack.pop()
    val a = stack.pop()
    return if (b is Lambda) {
        val list = ArrayList<Any>()
        iterator(a).forEachRemaining { e: Any -> list.add(e) }
        list.sortWith { o1: Any, o2: Any ->
            val r1 = sortByFunctionHelper(b.call(o1))
            val r2 = sortByFunctionHelper(b.call(o2))
            r1.compareTo(r2)
        }
        JyxalList.create(list)
    } else if (a is BigComplex && b is BigComplex) {
        JyxalList.range(a, b)
    } else {
        val split = regexCache.computeIfAbsent(b.toString(), Pattern::compile).split(a.toString())
        JyxalList.create(split)
    }
}

private fun sortByFunctionHelper(obj: Any): BigComplex {
    return when (obj) {
        is BigComplex -> {
            obj
        }
        is JyxalList -> {
            BigComplex.valueOf(obj.size.toLong())
        }
        else -> {
            BigComplex.valueOf(obj.toString().length.toLong())
        }
    }
}

fun splitOn(stack: ProgramStack): Any {
    val b = stack.pop()
    val a = stack.pop()
    return if (a is JyxalList) {
        val superList = JyxalList.create()
        var newList = JyxalList.create()
        for (item in a) {
            if (item == b) {
                superList.add(newList)
                newList = JyxalList.create()
            } else {
                newList.add(item)
            }
        }
        superList.add(newList)
        superList
    } else {
        JyxalList.create(a.toString().split(b.toString()))
    }
}

fun subtract(stack: ProgramStack): Any {
    val o = vectorise(2, ::subtract, stack)
    if (o != null) return o
    val b = stack.pop()
    val a = stack.pop()
    return if (a is BigComplex) {
        if (b is BigComplex) {
            a.subtract(b)
        } else "-".repeat(a.re.toInt()) + b
    } else if (b is BigComplex) {
        a.toString() + "-".repeat(b.re.toInt())
    } else {
        a.toString().replace(b.toString(), "")
    }
}

fun sum(obj: Any): Any {
    return when (obj) {
        is JyxalList -> {
            var sum: Any = BigComplex.ZERO
            for (item in obj) {
                sum = addImpl(sum, item)
            }
            sum
        }
        is BigComplex -> {
            val chars = PLUS_SPACE_I_PATTERN.matcher(obj.toString()).replaceAll("").toCharArray()
            var sum: Long = 0
            for (c in chars) {
                sum += (c.code - 48).toLong()
            }
            BigComplex.valueOf(sum)
        }
        else -> {
            var sum: Long = 0
            for (c in obj.toString().toCharArray()) {
                sum += c.code.toLong()
            }
            BigComplex.valueOf(sum)
        }
    }
}

fun tail(obj: Any): Any {
    return if (obj is JyxalList) {
        if (obj.isLazy()) {
            val iterator = obj.iterator()
            var last: Any = BigComplex.ZERO
            while (iterator.hasNext()) {
                last = iterator.next()
            }
            last
        } else {
            obj[obj.size - 1]
        }
    } else {
        val s = obj.toString()
        if (s.isEmpty()) {
            BigComplex.ZERO
        } else {
            s[s.length - 1]
        }
    }
}

fun triplicate(stack: ProgramStack): Any {
    val obj = stack.pop()
    stack.push(copy(obj))
    stack.push(copy(obj))
    return obj
}

fun twoPow(obj: Any): Any {
    return if (obj is BigComplex) {
        BigComplexMath.pow(BigComplex.TWO, obj, MathContext.DECIMAL128)
    } else {
        exec(obj.toString())
    }
}

fun monadVectorise(obj: Any, handle: MethodHandle): Any {
    if (obj is JyxalList) {
        val result = JyxalList.create()
        for (item in obj) {
            result.add(monadVectorise(item, handle))
        }
        return result
    }
    return handle.invoke(obj)
}

fun List<Any>.jyxal() = JyxalList.fromIterableLazy(this)

operator fun BigComplex.plus(other: BigComplex): BigComplex = this.add(other)
operator fun BigComplex.plus(other: Long): BigComplex = this.add(BigComplex.valueOf(other))
operator fun BigComplex.minus(other: BigComplex): BigComplex = this.subtract(other)
operator fun BigComplex.minus(other: Long): BigComplex = this.subtract(BigComplex.valueOf(other))
operator fun BigComplex.times(other: BigComplex): BigComplex = this.multiply(other)
operator fun BigComplex.times(other: Long): BigComplex = this.multiply(BigComplex.valueOf(other))

infix fun BigComplex.loga(a: BigComplex): BigComplex {
    // complex numbers go brr
    // going off of that fact that ln(x)/ln(a) = loga(x)
    // and ln(x) = ln(|x|) + i * arg(x)
    val top = BigComplexMath.log(this, MathContext.DECIMAL128)
    val bottom = BigComplexMath.log(a, MathContext.DECIMAL128)
    return top.divide(bottom, MathContext.DECIMAL128)
}

infix fun BigComplex.loga(a: Long): BigComplex = this.loga(BigComplex.valueOf(a))