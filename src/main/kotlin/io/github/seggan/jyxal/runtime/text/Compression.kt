package io.github.seggan.jyxal.runtime.text

import io.github.seggan.jyxal.runtime.fromBaseDigitsAlphabet
import io.github.seggan.jyxal.runtime.times
import io.github.seggan.jyxal.runtime.toBaseDigitsAlphabet
import java.util.ArrayDeque
import java.util.Deque
import kotlin.math.max

/**
 * Compression related stuff
 */
object Compression {

    const val CODEPAGE = "λƛ¬∧⟑∨⟇÷×«\n»°•ß†€½∆ø↔¢⌐æʀʁɾɽÞƈ∞¨ !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]`^_abcdefghijklmnopqrstuvwxyz{|}~↑↓∴∵›‹∷¤ð→←βτȧḃċḋėḟġḣḭŀṁṅȯṗṙṡṫẇẋẏż√⟨⟩‛₀₁₂₃₄₅₆₇₈¶⁋§ε¡∑¦≈µȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ₌₍⁰¹²∇⌈⌊¯±₴…□↳↲⋏⋎꘍ꜝ℅≤≥≠⁼ƒɖ∪∩⊍£¥⇧⇩ǍǎǏǐǑǒǓǔ⁽‡≬⁺↵⅛¼¾Π„‟"
    private const val COMPRESSION_CODEPAGE = "λƛ¬∧⟑∨⟇÷×«»°•ß†€½∆ø↔¢⌐æʀʁɾɽÞƈ∞¨↑↓∴∵›‹∷¤ð→←βτȧḃċḋėḟġḣḭŀṁṅȯṗṙṡṫẇẋẏż√⟨⟩‛₀₁₂₃₄₅₆₇₈¶⁋§ε¡∑¦≈µȦḂĊḊĖḞĠḢİĿṀṄȮṖṘṠṪẆẊẎŻ₌₍⁰¹²∇⌈⌊¯±₴…□↳↲⋏⋎꘍ꜝ℅≤≥≠⁼ƒɖ∪∩⊍£¥⇧⇩ǍǎǏǐǑǒǓǔ⁽‡≬⁺↵⅛¼¾Π„‟"

    private val longDict: Array<String> by lazy {
        val s: String
        Compression::class.java.classLoader.getResourceAsStream("dictLong.txt")
                .use { `in` -> s = String(`in`!!.readAllBytes()) }
        s.replace("\r", "").split("\n").toTypedArray()
    }
    private val shortDict: Array<String> by lazy {
        val s: String
        Compression::class.java.classLoader.getResourceAsStream("dictShort.txt")
                .use { `in` -> s = String(`in`!!.readAllBytes()) }
        s.replace("\r", "").split("\n").toTypedArray()
    }
    private val maxWordLen by lazy {
        longDict.maxByOrNull { it.length }!!.length
    }
    private val lookupDict by lazy {
        val dict = mutableMapOf<String, Int>()
        var i = 0
        longDict.forEach {
            dict[it] = i++
        }
        dict
    }

    @JvmStatic
    fun decompress(str: String): String {
        val sb = StringBuilder()
        val temp = StringBuilder()
        val chars: Deque<Char> = ArrayDeque(str.length)
        for (c in str) {
            chars.add(c)
        }
        while (!chars.isEmpty()) {
            val c = chars.removeFirst()
            if (COMPRESSION_CODEPAGE.indexOf(c) != -1) {
                temp.append(c)
                if (temp.length == 2) {
                    val index = fromBaseDigitsAlphabet(temp, COMPRESSION_CODEPAGE)
                    val dict = longDict
                    if (index < dict.size) {
                        sb.append(dict[index])
                    }
                    temp.setLength(0)
                }
            } else {
                if (temp.isNotEmpty()) {
                    val index = COMPRESSION_CODEPAGE.indexOf(temp.toString())
                    if (index < shortDict.size) {
                        sb.append(shortDict[index])
                    }
                    temp.setLength(0)
                    if (c == ' ') {
                        continue
                    }
                }
                sb.append(c)
            }
        }
        if (temp.isNotEmpty()) {
            val index = fromBaseDigitsAlphabet(temp, COMPRESSION_CODEPAGE)
            val dict = shortDict
            if (index < dict.size) {
                sb.append(dict[index])
            }
        }
        return sb.toString()
    }

    fun compress(input: String): String {
        val dp = mutableListOf(" " * (input.length + 1)) * (input.length + 1)
        dp[0] = ""
        for (ind in 1 until (input.length + 1)) {
            for (left in max(0, ind - maxWordLen) until (ind - 1)) {
                val i = wordIndex(input.slice(left until ind))
                if (i != null) {
                    dp[ind] = listOf(dp[ind], dp[left] + i).minByOrNull(String::length)!!
                    break
                }
            }
            dp[ind] = listOf(dp[ind], dp[ind - 1] + input[ind - 1]).minByOrNull(String::length)!!
        }
        val result = dp.last()
        if (result.length == 2) {
            return "‛$result"
        }
        return "`$result`"
    }

    fun decompressSmaz(input: String): String {
        val arr = ByteArray(input.length)
        for (i in input.indices) {
            arr[i] = (CODEPAGE.indexOf(input[i]) - 128).toByte()
        }
        return Smaz().decompress(arr)
    }

    fun compressSmaz(input: String): String {
        return buildString {
            val arr = Smaz().compress(input)
            for (c in arr) {
                append(CODEPAGE[c.toInt() + 128])
            }
        }
    }

    private fun wordIndex(word: String): String? {
        if (word in lookupDict) {
            val ret = toBaseDigitsAlphabet(lookupDict[word]!!.toLong(), COMPRESSION_CODEPAGE)
            if (ret.length == 1) {
                return "λ$ret"
            }
            return ret
        }
        return null
    }
}