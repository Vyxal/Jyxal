package io.github.seggan.jyxal.runtime.text

import io.github.seggan.jyxal.runtime.fromBaseDigitsAlphabet
import java.util.ArrayDeque
import java.util.Deque

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
}