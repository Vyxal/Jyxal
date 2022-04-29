@file:Suppress("MemberVisibilityCanBePrivate", "unused")
@file:JvmName("RuntimeHelpers")

package io.github.seggan.jyxal.runtime

import io.github.seggan.jyxal.runtime.list.JyxalList
import io.github.seggan.jyxal.runtime.math.BigComplex
import jdk.jshell.JShell
import jdk.jshell.Snippet
import java.math.BigDecimal
import java.math.BigInteger
import java.util.function.Supplier
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.sqrt

internal val VOWELS = setOf('a', 'e', 'i', 'o', 'u', 'y')
private val jShell: JShell by lazy(LazyThreadSafetyMode.NONE) {
    val shell = JShell.create()
    Runtime.getRuntime().addShutdownHook(Thread { shell.close() })
    shell
}
private val NUMBER_PATTERN: Pattern by lazy(LazyThreadSafetyMode.NONE) { Pattern.compile("\\d+(\\.\\d+)?") }
private val LIST_PATTERN: Pattern by lazy(LazyThreadSafetyMode.NONE) { Pattern.compile("\\[.+(?:,(.+))*]") }

fun applyLambda(lambda: Lambda, obj: Any): Any {
    when (obj) {
        is JyxalList -> {
            val newList = JyxalList.create()
            for (item in obj) {
                newList.add(applyLambda(lambda, item))
            }
            return newList
        }
        is BigComplex -> {
            val current = BigComplex.ONE
            val list = JyxalList.create()
            while (current <= obj) {
                list.add(lambda.call(current))
            }
            return list
        }
        else -> {
            val s = obj.toString()
            val list = JyxalList.create()
            for (c in s) {
                list.add(lambda.call(c.toString()))
            }
            return list
        }
    }
}

fun copy(obj: Any): Any {
    return if (obj is JyxalList) {
        obj.map(::copy)
    } else obj
}

fun exec(expr: String): Any {
    val stack = ProgramStack()
    for (e in jShell.eval(jShell.sourceCodeAnalysis().analyzeCompletion(expr).source())) {
        if (e.status() == Snippet.Status.VALID) {
            stack.push(eval(e.value()))
        } else {
            throw RuntimeException(e.toString())
        }
    }
    return stack.pop()
}

fun escapeString(str: String): String {
    val sb = StringBuilder()
    for (c in str) {
        when (c) {
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\$' -> sb.append("\\$")
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

fun eval(expr: String): Any {
    return if (NUMBER_PATTERN.matcher(expr).matches()) {
        BigComplex.valueOf(BigDecimal(expr))
    } else {
        val matcher: Matcher = LIST_PATTERN.matcher(expr)
        return if (matcher.matches()) {
            val list = JyxalList.create()
            while (matcher.find()) {
                list.add(eval(matcher.group(1)))
            }
            list
        } else if (expr.startsWith("\"") && expr.endsWith("\"")) {
            expr.substring(1, expr.length - 1)
        } else {
            expr
        }
    }
}

fun filterLambda(lambda: Lambda, obj: Any): Any {
    return when (obj) {
        is JyxalList -> {
            obj.filter { truthValue(lambda.call(it)) }
        }
        is BigComplex -> {
            JyxalList.range(BigComplex.ONE, obj).filter { truthValue(lambda.call(it)) }
        }
        else -> {
            val s = obj.toString()
            val list = JyxalList.create()
            for (c in s) {
                if (truthValue(lambda.call(c.toString()))) {
                    list.add(c.toString())
                }
            }
            list
        }
    }
}

fun forify(stack: ProgramStack): Iterator<Any> {
    return forify(stack.pop())
}

fun forify(obj: Any): Iterator<Any> {
    return if (obj is BigComplex) {
        return object : Iterator<Any> {
            private var current = BigComplex.ONE

            override fun hasNext(): Boolean {
                return current.re <= obj.re
            }

            override fun next(): Any {
                val next = current
                current = current.plus(BigComplex.ONE)
                return next
            }
        }
    } else {
        iterator(obj)
    }
}

fun fromBaseDigitsAlphabet(digits: CharSequence, alphabet: String): Int {
    var result = 0
    for (element in digits) {
        result = result * alphabet.length + alphabet.indexOf(element)
    }
    return result
}

fun iterator(obj: Any): Iterator<Any> {
    if (obj is JyxalList) {
        return obj.iterator()
    } else {
        val s = obj.toString()
        return object : Iterator<Any> {
            private var index = 0

            override fun hasNext(): Boolean {
                return index < s.length
            }

            override fun next(): Any {
                return s[index++].toString()
            }
        }
    }
}

fun len(obj: Any): Int {
    return if (obj is JyxalList) {
        obj.size
    } else {
        obj.toString().length
    }
}

fun listify(obj: Any): JyxalList {
    return if (obj is JyxalList) {
        obj
    } else {
        JyxalList.create(iterator(obj))
    }
}

fun mapLambda(lambda: Lambda, obj: Any): Any {
    return when (obj) {
        is JyxalList -> {
            obj.map { lambda.call(it) }
        }
        is BigComplex -> {
            JyxalList.range(BigComplex.ONE, obj).map { lambda.call(it) }
        }
        else -> {
            val s = obj.toString()
            val list = JyxalList.create()
            for (c in s) {
                list.add(lambda.call(c.toString()))
            }
            list
        }
    }
}

fun <T : MutableCollection<BigComplex>> primeFactors(n: BigComplex, factory: Supplier<T>): T {
    val factors = factory.get()
    if (n.re < BigDecimal.valueOf(Long.MAX_VALUE) && n.re > BigDecimal.valueOf(Long.MIN_VALUE)) {
        // we can use primitives to speed this up
        val nLong = n.re.toLong()
        val sqrt = sqrt(nLong.toDouble()).toLong()
        for (i in 2..sqrt) {
            if (nLong % i == 0L) {
                factors.add(BigComplex.valueOf(i))
            }
        }
        if (sqrt * sqrt == nLong) {
            factors.add(BigComplex.valueOf(sqrt))
        }
    } else {
        // we can't use primitives, so we'll have to use BigInteger
        val nBig = n.re.toBigInteger()
        val sqrt = nBig.sqrt()
        var i = BigInteger.valueOf(2)
        while (i <= sqrt) {
            if (nBig.mod(i) == BigInteger.ZERO) {
                factors.add(BigComplex.valueOf(BigDecimal(i)))
            }
            i += BigInteger.ONE
        }
        if (sqrt.multiply(sqrt) == nBig) {
            factors.add(BigComplex.valueOf(BigDecimal(sqrt)))
        }
    }
    return factors
}

fun repeatCharacters(str: String, times: Int): String {
    val sb = StringBuilder()
    for (c in str) {
        sb.append(c.toString().repeat(0.coerceAtLeast(times)))
    }
    return sb.toString()
}

fun replacementIterator(iterator: Iterator<Any>, index: Int, replacement: Any): Iterator<Any> {
    return object : Iterator<Any> {
        private var i = 0

        override fun hasNext(): Boolean {
            return iterator.hasNext()
        }

        override fun next(): Any {
            return if (i++ == index) {
                replacement
            } else {
                iterator.next()
            }
        }
    }
}

fun toBaseDigits(integer: BigInteger, base: BigInteger): List<BigInteger> {
    val ret = mutableListOf<BigInteger>()
    var n = integer
    while (n >= base) {
        val (newN, digit) = n.divideAndRemainder(base)
        ret.add(digit)
        n = newN
    }
    ret.add(n)
    return ret.reversed()
}

fun toBaseDigitsAlphabet(integer: Long, alphabet: String): String {
    val sb = StringBuilder()
    var n = integer
    val base = alphabet.length
    while (n >= base) {
        val div = n / base
        sb.append(alphabet[(n % base).toInt()])
        n = div
    }
    sb.append(alphabet[n.toInt()])
    return sb.reverse().toString()
}

fun truthValue(stack: ProgramStack): Boolean {
    return truthValue(stack.pop())
}

fun unescapeString(st: String): String {
    val sb = StringBuilder(st.length)
    var i = 0
    while (i < st.length) {
        var ch = st[i]
        if (ch == '\\') {
            val nextChar = if (i == st.length - 1) '\\' else st[i + 1]
            // Octal escape?
            if (nextChar in '0'..'7') {
                var code = "" + nextChar
                i++
                if (i < st.length - 1 && st[i + 1] >= '0' && st[i + 1] <= '7') {
                    code += st[i + 1]
                    i++
                    if (i < st.length - 1 && st[i + 1] >= '0' && st[i + 1] <= '7') {
                        code += st[i + 1]
                        i++
                    }
                }
                sb.append(code.toInt(8).toChar())
                i++
                continue
            }
            when (nextChar) {
                '\\' -> ch = '\\'
                'b' -> ch = '\b'
                'n' -> ch = '\n'
                'r' -> ch = '\r'
                't' -> ch = '\t'
                '\"' -> ch = '\"'
                '\'' -> ch = '\''
                'u' -> {
                    if (i >= st.length - 5) {
                        break
                    } else {
                        val code = ("" + st[i + 2] + st[i + 3] + st[i + 4] + st[i + 5]).toInt(16)
                        sb.append(Character.toChars(code))
                        i += 5
                        continue
                    }
                }
            }
            i++
        }
        sb.append(ch)
        i++
    }
    return sb.toString()
}


fun truthValue(obj: Any): Boolean {
    if (obj is JyxalList) {
        return obj.isNotEmpty()
    } else if (obj is BigComplex) {
        return obj != BigComplex.ZERO
    }

    return true
}

fun vectorise(arity: Int, function: (ProgramStack) -> Any, stack: ProgramStack): Any? {
    when (arity) {
        1 -> {
            val obj = stack.pop()
            if (obj is JyxalList) {
                return obj.map { function(ProgramStack(it)) }
            }
            stack.push(obj)
        }
        2 -> {
            val right = stack.pop()
            val left = stack.pop()
            if (left is JyxalList) {
                if (right is JyxalList) {
                    return left.zip(right) { a, b -> function(ProgramStack(a, b)) }
                }
                return left.map { function(ProgramStack(it, right)) }
            } else if (right is JyxalList) {
                return right.map { function(ProgramStack(left, it)) }
            }
            stack.push(left)
            stack.push(right)
        }
        3 -> {
            val right = stack.pop()
            val middle = stack.pop()
            val left = stack.pop()
            if (left is JyxalList) {
                if (middle is JyxalList) {
                    if (right is JyxalList) {
                        return left.zip(middle).zip(right) { a, b ->
                            a as JyxalList
                            function(ProgramStack(a[0], a[1], b))
                        }
                    }
                    return left.zip(middle) { a, b -> function(ProgramStack(a, b, right)) }
                } else if (right is JyxalList) {
                    return left.zip(right) { a, b -> function(ProgramStack(a, middle, b)) }
                }
                return left.map { function(ProgramStack(it, middle, right)) }
            } else if (middle is JyxalList) {
                if (right is JyxalList) {
                    return middle.zip(right) { a, b -> function(ProgramStack(left, a, b)) }
                }
                return middle.map { function(ProgramStack(left, it, right)) }
            } else if (right is JyxalList) {
                return right.map { function(ProgramStack(left, middle, it)) }
            }
            stack.push(left)
            stack.push(middle)
            stack.push(right)
        }
        else -> throw RuntimeException("Vectorise arity $arity not supported")
    }
    return null
}
