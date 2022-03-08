package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

public class ProgramStack extends ArrayDeque<Object> implements Deque<Object> {

    private final Object[] input;

    public ProgramStack() {
        super();
        this.input = null;
    }

    public ProgramStack(Object... objects) {
        super(List.of(objects));
        this.input = null;
    }

    public ProgramStack(Collection<?> c) {
        super(c);
        this.input = null;
    }

    public void swap() {
        Object a = this.pop();
        Object b = this.pop();
        this.push(a);
        this.push(b);
    }

    public void push(boolean b) {
        push(BigComplex.valueOf(b));
    }

    public void push(int i) {
        push(BigComplex.valueOf(i));
    }

}
