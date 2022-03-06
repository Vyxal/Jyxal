package io.github.seggan.jyxal.runtime;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class LazyInit<T> {

    private final Supplier<T> supplier;
    private volatile T value = null;

    public LazyInit(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public static LazyInit<Pattern> regex(String regex) {
        return new LazyInit<>(() -> Pattern.compile(regex));
    }

    public T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }
}
