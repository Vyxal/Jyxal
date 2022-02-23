package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

public class ProgramStack extends ArrayDeque<Object> implements Deque<Object> {

    public ProgramStack() {
        super();
    }

    public ProgramStack(Object... objects) {
        super(List.of(objects));
    }

    public ProgramStack(Collection<?> c) {
        super(c);
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
