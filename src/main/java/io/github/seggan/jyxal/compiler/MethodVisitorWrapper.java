package io.github.seggan.jyxal.compiler;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class MethodVisitorWrapper extends MethodVisitor implements Opcodes {

    private final int stackVar;
    private final int ctxVar;

    private final Set<Integer> reservedVars = new HashSet<>();

    public MethodVisitorWrapper(ClassWriter cw, int access, String name, String desc) {
        super(Opcodes.ASM7, cw.visitMethod(access, name, desc, null, null));
        this.stackVar = Type.getArgumentTypes(desc).length;
        this.ctxVar = stackVar + 1;

        visitTypeInsn(NEW, "runtime/ProgramStack");
        visitInsn(DUP);
        visitMethodInsn(INVOKESPECIAL, "runtime/ProgramStack", "<init>", "()V", false);
        visitVarInsn(ASTORE, stackVar);

        AsmHelper.addBigComplex("0", this);
        visitVarInsn(ASTORE, ctxVar);
    }

    public int getStackVar() {
        return stackVar;
    }

    public int getCtxVar() {
        return ctxVar;
    }

    public int reserveVar() {
        Optional<Integer> optional = max(reservedVars);
        if (optional.isEmpty()) {
            int var = ctxVar + 1;
            reservedVars.add(var);
            return var;
        }
        int max = optional.get();
        for (int i = ctxVar + 1; i < max; i++) {
            if (!reservedVars.contains(i)) {
                reservedVars.add(i);
                return i;
            }
        }
        int newVar = max + 1;
        reservedVars.add(newVar);
        return newVar;
    }

    public void freeVar(int var) {
        reservedVars.remove(var);
    }

    private static <T extends Comparable<T>> Optional<T> max(Collection<T> collection) {
        if (collection.isEmpty()) {
            return Optional.empty();
        }
        T max = collection.iterator().next();
        for (T t : collection) {
            if (t.compareTo(max) > 0) {
                max = t;
            }
        }
        return Optional.of(max);
    }
}
