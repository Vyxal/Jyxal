package io.github.seggan.jyxal.runtime;

/**
 * Some methods that Java just gets faster than Kotlin
 */
public class JavaNatives {

    private JavaNatives() {}

    public static String repeat(String s, int n) {
        return s.repeat(n);
    }
}
