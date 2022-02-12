package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.math.BigComplex;

public final class MathMethods {

    private MathMethods() {
    }

    public static void add(ProgramStack stack) {
        if (OtherMethods.vectorise(2, MathMethods::add, stack)) return;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            stack.push(ca.add(cb));
        } else {
            stack.push(a + b.toString());
        }
    }
}
