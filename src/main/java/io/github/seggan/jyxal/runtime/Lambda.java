package io.github.seggan.jyxal.runtime;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

public record Lambda(int arity, MethodHandle handle) {

    public Object call(ProgramStack stack) {
        List<Object> args = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            args.add(stack.pop());
        }

        try {
            return handle.invoke(new ProgramStack(args));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
