package io.github.seggan.jyxal.runtime;


import io.github.seggan.jyxal.runtime.math.BigComplex;

public final class MathMethods {

    private MathMethods() {
    }

    public static Object add(Object a, Object b) {
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            return ca.add(cb);
        } else {
            return a.toString() + b.toString();
        }
    }

    public static void doSomethignElse() {
        OtherMethods.doSomething();
    }
}
