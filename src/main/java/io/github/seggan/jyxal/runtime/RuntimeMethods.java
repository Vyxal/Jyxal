package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import io.github.seggan.jyxal.runtime.math.BigComplexMath;

import java.math.BigInteger;
import java.math.MathContext;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

public final class RuntimeMethods {

    private RuntimeMethods() {
    }

    public static void add(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::add, stack)) return;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            stack.push(ca.add(cb));
        } else {
            stack.push(a + b.toString());
        }
    }

    private static void compare(ProgramStack stack, IntPredicate predicate) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            stack.push(predicate.test(ca.compareTo(cb)));
        } else {
            stack.push(predicate.test(a.toString().compareTo(b.toString())));
        }
    }

    public static void duplicate(ProgramStack stack) {
        Object obj = Objects.requireNonNull(stack.peek());
        if (obj instanceof JyxalList jyxalList) {
            // deep copy
            stack.push(RuntimeHelpers.deepCopy(jyxalList));
        } else {
            stack.push(obj);
        }
    }

    public static void equals(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::equals, stack)) return;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            stack.push(ca.equals(cb));
        } else {
            stack.push(a.toString().equals(b.toString()));
        }
    }

    public static void functionCall(ProgramStack stack) {
        Object obj = stack.pop();
        if (obj instanceof Lambda lambda) {
            stack.push(lambda.call(stack));
        } else if (obj instanceof JyxalList list) {
            list.map(o -> BigComplex.valueOf(!RuntimeHelpers.truthValue(o)));
            stack.push(list);
        } else if (obj instanceof BigComplex complex) {
            stack.push(RuntimeHelpers.primeFactors(complex, HashSet::new).size());
        } else {
            stack.push(RuntimeHelpers.exec(obj.toString()));
        }
    }

    public static void infinitePrimes(ProgramStack stack) {
        stack.push(JyxalList.create(new Supplier<>() {
            BigInteger next = BigInteger.ONE;

            @Override
            public Object get() {
                next = next.nextProbablePrime();
                return next;
            }
        }));
    }

    public static void greaterThan(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::greaterThan, stack)) return;
        compare(stack, i -> i > 0);
    }

    public static void greaterThanOrEqual(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::greaterThanOrEqual, stack)) return;
        compare(stack, i -> i >= 0);
    }

    public static void halve(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(1, RuntimeMethods::halve, stack)) return;
        Object obj = stack.pop();
        if (obj instanceof BigComplex complex) {
            stack.push(complex.divide(BigComplex.TWO, MathContext.DECIMAL128));
        } else {
            String str = obj.toString();
            int limit = str.length() / 2 + 1;
            stack.push(str.substring(0, limit));
            stack.push(str.substring(limit));
        }
    }

    public static void itemSplit(ProgramStack stack) {
        Object obj = stack.pop();
        if (obj instanceof BigComplex complex) {
            obj = complex.re.toBigInteger().toString();
        }
        if (obj instanceof JyxalList list) {
            for (Object item : list) {
                stack.push(item);
            }
        } else {
            for (char c : obj.toString().toCharArray()) {
                stack.push(Character.toString(c));
            }
        }
    }

    public static void lessThan(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::lessThan, stack)) return;
        compare(stack, i -> i < 0);
    }

    public static void lessThanOrEqual(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::lessThanOrEqual, stack)) return;
        compare(stack, i -> i <= 0);
    }

    public static void logicalAnd(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (RuntimeHelpers.truthValue(a)) {
            if (RuntimeHelpers.truthValue(b)) {
                stack.push(a);
            } else {
                stack.push(b);
            }
        } else {
            stack.push(a);
        }
    }

    public static void logicalOr(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (RuntimeHelpers.truthValue(a)) {
            stack.push(a);
        } else {
            if (RuntimeHelpers.truthValue(b)) {
                stack.push(b);
            } else {
                stack.push(a);
            }
        }
    }

    public static void multiCommand(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::multiCommand, stack)) return;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca) {
            if (b instanceof BigComplex cb) {
                // complex numbers go brr
                // going off of that fact that ln(x)/ln(a) = loga(x)
                // and ln(x) = ln(|x|) + i * arg(x)
                BigComplex top = BigComplexMath.log(cb, MathContext.DECIMAL128);
                BigComplex bottom = BigComplexMath.log(ca, MathContext.DECIMAL128);
                stack.push(top.divide(bottom, MathContext.DECIMAL128));
            } else {
                stack.push(RuntimeHelpers.repeatCharacters(b.toString(), ca.re.intValue()));
            }
        } else if (b instanceof BigComplex cb) {
            stack.push(RuntimeHelpers.repeatCharacters(a.toString(), cb.re.intValue()));
        } else {
            StringBuilder sb = new StringBuilder();
            String aString = a.toString();
            String bString = b.toString();
            for (int i = 0; i < aString.length(); i++) {
                char ch = bString.charAt(i % bString.length());
                char aChar = aString.charAt(i);
                if (Character.isUpperCase(ch)) {
                    sb.append(Character.toUpperCase(aChar));
                } else if (Character.isLowerCase(ch)) {
                    sb.append(Character.toLowerCase(aChar));
                } else {
                    sb.append(aChar);
                }
            }

            if (aString.length() > bString.length()) {
                sb.append(aString.substring(bString.length()));
            }

            stack.push(sb.toString());
        }
    }

    public static void splitOn(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof JyxalList list) {
            JyxalList superList = JyxalList.create();
            JyxalList newList = JyxalList.create();
            for (Object item : list) {
                if (item.equals(b)) {
                    superList.add(newList);
                    newList = JyxalList.create();
                } else {
                    newList.add(item);
                }
            }
            superList.add(newList);
            stack.push(superList);
        } else {
            stack.push(JyxalList.create(a.toString().split(b.toString())));
        }
    }

    public static void triplicate(ProgramStack stack) {
        Object obj = Objects.requireNonNull(stack.peek());
        if (obj instanceof JyxalList jyxalList) {
            // deep copy
            stack.push(RuntimeHelpers.deepCopy(jyxalList));
            stack.push(RuntimeHelpers.deepCopy(jyxalList));
        } else {
            stack.push(obj);
            stack.push(obj);
        }
    }

}
