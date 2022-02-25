package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import io.github.seggan.jyxal.runtime.math.BigComplexMath;

import java.lang.invoke.MethodHandle;
import java.math.BigDecimal;
import java.math.BigInteger;
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

    public static Object isPrime(Object obj) {
        if (obj instanceof BigComplex complex) {
            BigInteger n = complex.re.toBigInteger();
            BigDecimal bsqrt = complex.re.sqrt(MathContext.DECIMAL128);
            if (n.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) < 0
                    && n.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) > 0) {
                long l = n.longValue();
                long sqrt = bsqrt.longValue();
                for (long i = 2; i <= sqrt; i++) {
                    if (l % i == 0) {
                        return BigComplex.valueOf(false);
                    }
                }
            } else {
                BigInteger sqrt = bsqrt.toBigInteger();
                for (BigInteger i = BigInteger.valueOf(2); i.compareTo(sqrt) <= 0; i = i.add(BigInteger.ONE)) {
                    if (n.mod(i).compareTo(BigInteger.ZERO) == 0) {
                        return BigComplex.valueOf(false);
                    }
                }
            }

            return BigComplex.valueOf(true);
        } else {
            String str = obj.toString();
            boolean isUppercase = Character.isUpperCase(str.charAt(0));
            for (char c : str.toCharArray()) {
                if (Character.isUpperCase(c) != isUppercase) {
                    return BigComplex.valueOf(-1);
                }
            }

            return BigComplex.valueOf(isUppercase);
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
