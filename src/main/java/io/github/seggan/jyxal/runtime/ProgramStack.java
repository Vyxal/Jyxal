package io.github.seggan.jyxal.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class ProgramStack extends ArrayDeque<Object> implements Deque<Object> {

    public ProgramStack() {
        super();
    }

    public ProgramStack(Object... objects) {
        super(List.of(objects));
    }

    public void swap() {
        Object a = this.pop();
        Object b = this.pop();
        this.push(a);
        this.push(b);
    }

}
