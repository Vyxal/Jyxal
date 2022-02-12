package io.github.seggan.jyxal.compiler.wrappers;

import org.objectweb.asm.Opcodes;

public class ContextualVariable implements AutoCloseable {

    final int index;
    private final JyxalMethod mv;

    private boolean isClosed = false;

    ContextualVariable(int index, JyxalMethod mv) {
        this.index = index;
        this.mv = mv;
    }

    @Override
    public void close() {
        checkClosed();
        mv.freeVar(this);
        isClosed = true;
    }

    public void load() {
        checkClosed();
        mv.visitVarInsn(Opcodes.ALOAD, index);
    }

    public void store() {
        checkClosed();
        mv.visitVarInsn(Opcodes.ASTORE, index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextualVariable that)) return false;

        if (index != that.index) return false;
        return isClosed == that.isClosed && mv.equals(that.mv);
    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + mv.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ContextualVariable{index=%d, isClosed=%s}".formatted(index, isClosed);
    }

    private void checkClosed() {
        if (isClosed) {
            throw new IllegalStateException("Already closed");
        }
    }
}
