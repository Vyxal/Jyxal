package io.github.seggan.jyxal.runtime;

import java.util.ArrayDeque;
import java.util.Deque;

public class ProgramStack extends ArrayDeque<Object> implements Deque<Object> {

    public ProgramStack() {
        super();
    }

    public void swap() {
        Object a = this.pop();
        Object b = this.pop();
        this.push(a);
        this.push(b);
    }

}
