package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.compiler.wrappers.JyxalMethod;
import io.github.seggan.jyxal.runtime.text.Compression;
import org.objectweb.asm.ClassWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class Constants {

    private static final Map<String, Consumer<JyxalMethod>> constants = new HashMap<>();

    static {
        string("kA", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        number("ke", "2.71828182845904523536028747135266249775724709369995");
        string("kf", "Fizz");
        string("kb", "Buzz");
        string("kF", "FizzBuzz");
        string("kH", "Hello, World!");
        string("kh", "Hello World");
        number("k1", 1000);
        number("k2", 10000);
        number("k3", 100000);
        number("k4", 1000000);
        string("ka", "abcdefghijklmnopqrstuvwxyz");
        string("kL", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        string("kd", "0123456789");
        string("k6", "0123456789abcdef");
        string("k^", "0123456789ABCDEF");
        string("ko", "01234567");
        string("kp", "!\"#$%&\\'()*+,-./:;<=>?@[\\]^_`{|}~");
        string("kP", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ \t\n\r\u000b\u000c");
        string("kw", " \t\n\r\u000b\u000c");
        string("kr", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        string("kB", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        string("kZ", "ZYXWVUTSRQPONMLKJIHGFEDCBA");
        string("kz", "zyxwvutsrqponmlkjihgfedcba");
        string("kl", "zyxwvutsrqponmlkjihgfedcbaZYXWVUTSRQPONMLKJIHGFEDCBA");
        number("ki", "3.14159265358979323846264338327950288419716939937510");
        string("kn", "NaN");
        number("kg", "1.61803398874989484820458683436563811772030917980576");
        string("k\u03B2", "{}[]<>()");
        string("k\u1E02", "()[]{}");
        string("k\u00DF", "()[]");
        string("k\u1E03", "([{");
        string("k\u2265", ")]}");
        string("k\u2264", "([{<");
        string("k\u03A0", ")]}>");
        string("kv", "aeiou");
        string("kV", "AEIOU");
        string("k\u2228", "aeiouAEIOU");
        string("k\u27C7", Compression.CODEPAGE);
        number("k\u1E2D", "4294967296");
        string("k/", "/\\");
        number("kR", 360);
        string("kW", "https://");
        string("k\u2105", "http://");
        string("k\u21B3", "https://www.");
        string("k\u00B2", "http://www.");
        number("k\u00B6", 512);
        number("k\u204B", 1024);
        number("k\u00A6", 2048);
        number("k\u1E44", 4096);
        number("k\u1E45", 8192);
        number("k\u00A1", 16384);
        number("k\u03B5", 32768);
        number("k\u20B4", 65536);
        number("k\u00D7", "2147483648");
        string("k\u2070", "bcdfghjklmnpqrstvwxyz");
        string("k\u00B9", "bcdfghjklmnpqrstvwxz");
        string("kT", "[]<>-+.,");
        string("k\u1E56", "([{<>}])");
        string("kS", "\u0D9E");
        number("k\u2082", 1048576);
        number("k\u2083", 1073741824);
        string("k\u222A", "aeiouy");
        string("k\u228D", "AEIOUY");
        string("k\u2229", "aeiouyAEIOUY");
        string("k\u1E58", "IVXLCDM");
    }

    public static void compile(String name, ClassWriter cw, JyxalMethod method) {
        if (constants.containsKey(name)) {
            constants.get(name).accept(method);
        } else {
            Element.getByText(name).compile(cw, method);
        }
    }

    private static void number(String name, int value) {
        registerConstant(name, mv -> AsmHelper.selectNumberInsn(mv, value));
    }

    private static void number(String name, String value) {
        registerConstant(name, mv -> AsmHelper.addBigComplex(value, mv));
    }

    private static void registerConstant(String name, Consumer<JyxalMethod> method) {
        constants.put(name, method);
    }

    private static void string(String name, String value) {
        registerConstant(name, mv -> mv.visitLdcInsn(value));
    }
}
