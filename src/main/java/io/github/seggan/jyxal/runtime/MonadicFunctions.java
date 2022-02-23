package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import io.github.seggan.jyxal.runtime.math.BigComplexMath;

import java.lang.invoke.MethodHandle;
import java.math.MathContext;

public final class MonadicFunctions {

    private MonadicFunctions() {
    }

    public static Object all(Object obj) {
        if (obj instanceof JyxalList list) {
            for (Object item : list) {
                if (!RuntimeHelpers.truthValue(item)) {
                    return BigComplex.valueOf(false);
                }
            }
            return true;
        } else if (obj instanceof String string) {
            for (char c : string.toCharArray()) {
                if (!RuntimeHelpers.VOWELS.contains(c)) {
                    return BigComplex.valueOf(false);
                }
            }
            return BigComplex.valueOf(true);
        }

        return BigComplex.valueOf(true);
    }

    public static Object chrOrd(Object obj) {
        if (obj instanceof BigComplex complex) {
            return Character.toString(complex.re.intValueExact());
        } else {
            String str = obj.toString();
            if (str.length() == 1) {
                return BigComplex.valueOf(str.charAt(0));
            } else {
                JyxalList list = JyxalList.create();
                for (char c : str.toCharArray()) {
                    list.add((int) c);
                }
                return list;
            }
        }
    }

    public static Object twoPow(Object obj) {
        if (obj instanceof BigComplex complex) {
            return BigComplexMath.pow(BigComplex.TWO, complex, MathContext.DECIMAL128);
        } else {
            return RuntimeHelpers.exec(obj.toString());
        }
    }

    public static Object vectorise(Object obj, MethodHandle handle) {
        return RuntimeHelpers.vectoriseOne(obj, o -> {
            try {
                return handle.invoke(o);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }
}
