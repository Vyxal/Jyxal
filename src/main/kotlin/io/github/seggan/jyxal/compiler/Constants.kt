package io.github.seggan.jyxal.compiler

import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod
import io.github.seggan.jyxal.runtime.text.Compression
import org.objectweb.asm.ClassWriter
import java.util.function.Consumer

object Constants {

    private val constants: MutableMap<String, Consumer<JyxalMethod>> = HashMap()

    fun compile(name: String, cw: ClassWriter, method: JyxalMethod) {
        if (constants.containsKey(name)) {
            constants[name]!!.accept(method)
        } else {
            Element.getByText(name).compile(cw, method)
        }
    }

    private fun number(name: String, value: Int) {
        registerConstant(name) { mv: JyxalMethod -> AsmHelper.selectNumberInsn(mv, value) }
    }

    private fun number(name: String, value: String) {
        registerConstant(name) { mv: JyxalMethod -> AsmHelper.addBigComplex(value, mv) }
    }

    private fun registerConstant(name: String, method: Consumer<JyxalMethod>) {
        constants[name] = method
    }

    private fun string(name: String, value: String) {
        registerConstant(name) { mv: JyxalMethod -> mv.visitLdcInsn(value) }
    }

    init {
        string("kA", "ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        number("ke", "2.71828182845904523536028747135266249775724709369995")
        string("kf", "Fizz")
        string("kb", "Buzz")
        string("kF", "FizzBuzz")
        string("kH", "Hello, World!")
        string("kh", "Hello World")
        number("k1", 1000)
        number("k2", 10000)
        number("k3", 100000)
        number("k4", 1000000)
        string("ka", "abcdefghijklmnopqrstuvwxyz")
        string("kL", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
        string("kd", "0123456789")
        string("k6", "0123456789abcdef")
        string("k^", "0123456789ABCDEF")
        string("ko", "01234567")
        string("kp", "!\"#$%&\'()*+,-./:;<=>?@[\\]^_`{|}~")
        string(
            "kP",
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ \t\n\r\u000b\u000c"
        )
        string("kw", " \t\n\r\u000b\u000c")
        string("kr", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
        string("kB", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")
        string("kZ", "ZYXWVUTSRQPONMLKJIHGFEDCBA")
        string("kz", "zyxwvutsrqponmlkjihgfedcba")
        string("kl", "zyxwvutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA")
        number("ki", "3.14159265358979323846264338327950288419716939937510")
        string("kn", "NaN")
        number("kg", "1.61803398874989484820458683436563811772030917980576")
        string("kβ", "{}[]<>()")
        string("kḂ", "()[]{}")
        string("kß", "()[]")
        string("kḃ", "([{")
        string("k≥", ")]}")
        string("k≤", "([{<")
        string("kΠ", ")]}>")
        string("kv", "aeiou")
        string("kV", "AEIOU")
        string("k∨", "aeiouAEIOU")
        string("k⟇", Compression.CODEPAGE)
        number("kḭ", "4294967296")
        string("k/", "/\\")
        number("kR", 360)
        string("kW", "https://")
        string("k℅", "http://")
        string("k↳", "https://www.")
        string("k²", "http://www.")
        number("k¶", 512)
        number("k⁋", 1024)
        number("k¦", 2048)
        number("kṄ", 4096)
        number("kṅ", 8192)
        number("k¡", 16384)
        number("kε", 32768)
        number("k₴", 65536)
        number("k×", "2147483648")
        string("k⁰", "bcdfghjklmnpqrstvwxyz")
        string("k¹", "bcdfghjklmnpqrstvwxz")
        string("kT", "[]<>-+.,")
        string("kṖ", "([{<>}])")
        string("kS", "ඞ")
        number("k₂", 1048576)
        number("k₃", 1073741824)
        string("k∪", "aeiouy")
        string("k⊍", "AEIOUY")
        string("k∩", "aeiouyAEIOUY")
        string("kṘ", "IVXLCDM")
    }
}