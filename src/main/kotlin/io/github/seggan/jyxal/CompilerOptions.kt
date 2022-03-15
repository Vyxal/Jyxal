package io.github.seggan.jyxal

import java.util.EnumSet

enum class CompilerOptions(private val c: Char) {
    VYXAL_CODEPAGE('V'),
    PRINT_DEBUG_TREE('D'),
    DONT_OPTIMISE('o'),
    DONT_VECTORISE_MONADS('v'),
    PRINT_TO_FILE('f');

    companion object {
        @JvmField
        var OPTIONS: MutableSet<CompilerOptions> = EnumSet.noneOf(CompilerOptions::class.java)
        fun fromString(s: String) {
            for (c in s.toCharArray()) {
                for (option in values()) {
                    if (option.c == c) {
                        OPTIONS.add(option)
                    }
                }
            }
        }
    }
}