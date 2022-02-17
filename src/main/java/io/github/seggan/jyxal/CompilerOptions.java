package io.github.seggan.jyxal;

import java.util.EnumSet;
import java.util.Set;

public enum CompilerOptions {

    VYXAL_CODEPAGE('v'),
    PRINT_DEBUG_TREE('d')
    ;

    char c;

    CompilerOptions(char c) {
        this.c = c;
    }

    public static Set<CompilerOptions> fromString(String s) {
        Set<CompilerOptions> options = EnumSet.noneOf(CompilerOptions.class);
        for (char c : s.toCharArray()) {
            for (CompilerOptions option : CompilerOptions.values()) {
                if (option.c == c) {
                    options.add(option);
                }
            }
        }

        return options;
    }
}
