package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

public class ProgramStack extends ArrayDeque<Object> implements Deque<Object> {

    private Object[] input;
    private String flags;

    private int index;

    public ProgramStack() {
        super();
        this.input = null;
        this.flags = null;
    }

    // the String[] is the program args
    public ProgramStack(String[] strings) throws IOException {
        super();
        if (strings.length > 0) {
            this.flags = strings[0];
            if (strings.length > 1) {
                if (this.flags.indexOf('f') != -1) {
                    this.input = new Object[1];
                    this.input[0] = Files.readString(Path.of(strings[1]));
                } else {
                    this.input = new Object[strings.length - 1];
                    for (int i = 1; i < strings.length; i++) {
                        this.input[i - 1] = RuntimeHelpers.eval(strings[i]);
                    }
                }
            }
        }
    }

    public ProgramStack(Object... objects) {
        super(List.of(objects));
        this.input = objects;
        this.flags = null;
    }

    public ProgramStack(Collection<?> c) {
        super(c);
        this.input = c.toArray();
        this.flags = null;
    }

    @Override
    public Object pop() {
        if (this.isEmpty()) {
            return getInput();
        } else {
            return super.pop();
        }
    }

    public void swap() {
        Object a = this.pop();
        Object b = this.pop();
        this.push(a);
        this.push(b);
    }

    /**
     * This modifies this stack
     */
    public ProgramStack reverse() {
        ProgramStack reversed = new ProgramStack();
        while (!this.isEmpty()) {
            reversed.push(this.pop());
        }
        reversed.input = this.input;
        reversed.flags = this.flags;
        return reversed;
    }

    public void push(boolean b) {
        push(BigComplex.valueOf(b));
    }

    public void push(long i) {
        push(BigComplex.valueOf(i));
    }

    public Object getInput() {
        if (input == null || input.length == 0) {
            return BigComplex.ZERO;
        } else {
            index %= input.length;
            return input[index++];
        }
    }
}
