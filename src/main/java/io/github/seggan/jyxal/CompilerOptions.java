package io.github.seggan.jyxal;

import java.util.EnumSet;
import java.util.Set;

public enum CompilerOptions {

    VYXAL_CODEPAGE('V'),
    PRINT_DEBUG_TREE('D'),
    DONT_OPTIMISE('o'),
    DONT_VECTORISE_MONADS('v'),
    PRINT_TO_FILE('f'),
    ;

    public static Set<CompilerOptions> OPTIONS = EnumSet.noneOf(CompilerOptions.class);

    private final char c;

    CompilerOptions(char c) {
        this.c = c;
    }

    public static void fromString(String s) {
        for (char c : s.toCharArray()) {
            for (CompilerOptions option : CompilerOptions.values()) {
                if (option.c == c) {
                    OPTIONS.add(option);
                }
            }
        }
    }
}
