package io.github.seggan.jyxal.runtime;

import java.util.function.Supplier;

public final class LazyInit<T> {

    private final Supplier<T> supplier;
    private volatile T value = null;

    public LazyInit(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public T get() {
        if (value == null) {
            value = supplier.get();
        }
        return value;
    }
}
