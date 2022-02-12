package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.util.Objects;

public final class RuntimeMethods {

    private RuntimeMethods() {
    }

    public static void add(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::add, stack)) return;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            stack.push(ca.add(cb));
        } else {
            stack.push(a + b.toString());
        }
    }

    public static void dup(ProgramStack stack) {
        Object obj = Objects.requireNonNull(stack.peek());
        if (obj instanceof JyxalList jyxalList) {
            // deep copy
            stack.push(deepCopy(jyxalList));
        } else {
            stack.push(obj);
        }
    }

    private static JyxalList deepCopy(JyxalList list) {
        JyxalList copy = JyxalList.create();
        for (Object obj : list) {
            if (obj instanceof JyxalList jyxalList) {
                copy.add(deepCopy(jyxalList));
            } else {
                copy.add(obj);
            }
        }
        return copy;
    }
}
