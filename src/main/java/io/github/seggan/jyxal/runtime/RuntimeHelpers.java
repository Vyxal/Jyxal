package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuntimeHelpers {

    public static final Set<Character> VOWELS = Set.of('a', 'e', 'i', 'o', 'u', 'y');
    public static final LazyInit<JShell> jShell = new LazyInit<>(() -> {
        JShell shell = JShell.create();
        Runtime.getRuntime().addShutdownHook(new Thread(shell::close));
        return shell;
    });
    public static final LazyInit<Pattern> NUMBER_PATTERN = new LazyInit<>(() -> Pattern.compile("\\d+(\\.\\d+)?"));
    public static final LazyInit<Pattern> LIST_PATTERN = new LazyInit<>(() -> Pattern.compile("\\[.+(?:,(.+))*]"));

    private RuntimeHelpers() {
    }

    public static JyxalList deepCopy(JyxalList list) {
        JyxalList copy = JyxalList.create();
        for (Object obj : list) {
            if (obj instanceof JyxalList jyxalList) {
                copy.add(deepCopy(jyxalList));
            } else {
                copy.add(obj);
            }
        }
        return copy;
    }

    public static Object exec(String expr) {
        ProgramStack stack = new ProgramStack();
        for (SnippetEvent e : jShell.get().eval(jShell.get().sourceCodeAnalysis().analyzeCompletion(expr).source())) {
            if (e.status() == Snippet.Status.VALID) {
                pushExpr(stack, e.value());
            } else {
                throw new RuntimeException(e.toString());
            }
        }
        return stack.pop();
    }

    public static Iterator<Object> forify(ProgramStack stack) {
        return forify(stack.pop());
    }

    public static Iterator<Object> forify(Object obj) {
        if (obj instanceof JyxalList jyxalList) {
            return jyxalList.iterator();
        } else if (obj instanceof BigComplex bigComplex) {
            return new Iterator<>() {
                private BigComplex current = BigComplex.ONE;

                @Override
                public boolean hasNext() {
                    return current.re.compareTo(bigComplex.re) <= 0;
                }

                @Override
                public Object next() {
                    BigComplex next = current;
                    current = current.add(BigComplex.ONE);
                    return next;
                }
            };
        } else {
            String s = obj.toString();
            return new Iterator<>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < s.length();
                }

                @Override
                public Object next() {
                    return Character.toString(s.charAt(i++));
                }
            };
        }
    }

    public static <T extends Collection<BigComplex>> T primeFactors(BigComplex n, Supplier<T> factory) {
        T factors = factory.get();
        if (n.re.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) < 0
                && n.re.compareTo(BigDecimal.valueOf(Long.MIN_VALUE)) > 0) {
            // we can use primitives to speed this up
            long nLong = n.re.longValue();
            long sqrt = (long) Math.sqrt(nLong);
            for (long i = 2; i <= sqrt; i++) {
                if (nLong % i == 0) {
                    factors.add(BigComplex.valueOf(i));
                }
            }
            if (sqrt * sqrt == nLong) {
                factors.add(BigComplex.valueOf(sqrt));
            }
        } else {
            // we can't use primitives, so we'll have to use BigInteger
            BigInteger nBig = n.re.toBigInteger();
            BigInteger sqrt = nBig.sqrt();
            for (BigInteger i = BigInteger.valueOf(2); i.compareTo(sqrt) <= 0; i = i.add(BigInteger.ONE)) {
                if (nBig.mod(i).equals(BigInteger.ZERO)) {
                    factors.add(BigComplex.valueOf(new BigDecimal(i)));
                }
            }
            if (sqrt.multiply(sqrt).equals(nBig)) {
                factors.add(BigComplex.valueOf(new BigDecimal(sqrt)));
            }
        }

        return factors;
    }

    public static void pushExpr(ProgramStack stack, String expr) {
        if (NUMBER_PATTERN.get().matcher(expr).matches()) {
            stack.push(BigComplex.valueOf(new BigDecimal(expr)));
        } else {
            Matcher matcher = LIST_PATTERN.get().matcher(expr);
            if (matcher.matches()) {
                ProgramStack newStack = new ProgramStack();
                while (matcher.find()) {
                    pushExpr(newStack, matcher.group());
                }
                stack.push(JyxalList.create(newStack));
            } else if (expr.startsWith("\"") && expr.endsWith("\"")) {
                stack.push(expr.substring(1, expr.length() - 1));
            } else {
                stack.push(expr);
            }
        }
    }

    public static String repeatCharacters(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append(String.valueOf(c).repeat(Math.max(0, times)));
        }

        return sb.toString();
    }

    private static int slen(JyxalList first, JyxalList... rest) {
        int size = first.size();
        for (JyxalList list : rest) {
            size = Math.min(size, list.size());
        }
        return size;
    }

    public static boolean truthValue(ProgramStack stack) {
        return truthValue(stack.pop());
    }

    public static boolean truthValue(Object obj) {
        if (obj instanceof JyxalList jyxalList) {
            return jyxalList.size() != 0;
        } else if (obj instanceof BigComplex bigComplex) {
            return !bigComplex.equals(BigComplex.ZERO);
        }

        return true;
    }

    public static Object vectorise(int arity, Function<ProgramStack, Object> function, ProgramStack stack) {
        // <editor-fold desc="vectorise" defaultstate="collapsed">
        switch (arity) {
            case 1 -> {
                Object obj = stack.pop();
                if (obj instanceof JyxalList jyxalList) {
                    return jyxalList.map(o -> function.apply(new ProgramStack(o)));
                }
                stack.push(obj);
            }
            case 2 -> {
                Object right = stack.pop();
                Object left = stack.pop();
                if (left instanceof JyxalList leftList) {
                    if (right instanceof JyxalList rightList) {
                        JyxalList result = JyxalList.create();
                        int size = slen(leftList, rightList);
                        for (int i = 0; i < size; i++) {
                            result.add(function.apply(new ProgramStack(leftList.get(i), rightList.get(i))));
                        }
                        return result;
                    }
                    return leftList.map(o -> function.apply(new ProgramStack(o, right)));
                } else if (right instanceof JyxalList rightList) {
                    return rightList.map(o -> function.apply(new ProgramStack(left, o)));
                }
                stack.push(left);
                stack.push(right);
            }
            case 3 -> {
                Object right = stack.pop();
                Object middle = stack.pop();
                Object left = stack.pop();
                if (left instanceof JyxalList leftList) {
                    if (middle instanceof JyxalList middleList) {
                        if (right instanceof JyxalList rightList) {
                            JyxalList result = JyxalList.create();
                            int size = slen(leftList, middleList, rightList);
                            for (int i = 0; i < size; i++) {
                                result.add(function.apply(new ProgramStack(leftList.get(i), middleList.get(i), rightList.get(i))));
                            }
                            return result;
                        }
                        JyxalList result = JyxalList.create();
                        int size = slen(leftList, middleList);
                        for (int i = 0; i < size; i++) {
                            result.add(function.apply(new ProgramStack(leftList.get(i), middleList.get(i), right)));
                        }
                        return result;
                    }
                    if (right instanceof JyxalList rightList) {
                        JyxalList result = JyxalList.create();
                        int size = slen(leftList, rightList);
                        for (int i = 0; i < size; i++) {
                            result.add(function.apply(new ProgramStack(leftList.get(i), middle, rightList.get(i))));
                        }
                        return result;
                    }
                    return leftList.map(o -> function.apply(new ProgramStack(o, middle, right)));
                } else if (middle instanceof JyxalList middleList) {
                    if (right instanceof JyxalList rightList) {
                        JyxalList result = JyxalList.create();
                        int size = slen(middleList, rightList);
                        for (int i = 0; i < size; i++) {
                            result.add(function.apply(new ProgramStack(left, middleList.get(i), rightList.get(i))));
                        }
                        return result;
                    }
                    JyxalList result = JyxalList.create();
                    int size = slen(middleList);
                    for (int i = 0; i < size; i++) {
                        result.add(function.apply(new ProgramStack(left, middleList.get(i), right)));
                    }
                    return result;
                } else if (right instanceof JyxalList rightList) {
                    JyxalList result = JyxalList.create();
                    int size = slen(rightList);
                    for (int i = 0; i < size; i++) {
                        result.add(function.apply(new ProgramStack(left, middle, rightList.get(i))));
                    }
                    return result;
                }
                stack.push(left);
                stack.push(middle);
                stack.push(right);
            }
        }

        return null;
        // </editor-fold>
    }
}
