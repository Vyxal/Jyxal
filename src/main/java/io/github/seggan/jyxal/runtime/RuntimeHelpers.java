package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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

    public static Object applyLambda(Lambda lambda, Object obj) {
        if (obj instanceof JyxalList jyxalList) {
            JyxalList list = JyxalList.create();
            for (Object o : jyxalList) {
                list.add(lambda.call(o));
            }
            return list;
        } else if (obj instanceof BigComplex bigComplex) {
            BigComplex current = BigComplex.ONE;
            JyxalList list = JyxalList.create();
            while (current.compareTo(bigComplex) <= 0) {
                list.add(lambda.call(current));
            }
            return list;
        } else {
            String s = obj.toString();
            JyxalList list = JyxalList.create();
            for (char c : s.toCharArray()) {
                list.add(lambda.call(Character.toString(c)));
            }
            return list;
        }
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

    public static Object filterLambda(Lambda lambda, Object obj) {
        if (obj instanceof JyxalList jyxalList) {
            return jyxalList.filter(o -> truthValue(lambda.call(o)));
        } else if (obj instanceof BigComplex bigComplex) {
            return JyxalList.range(BigComplex.ONE, bigComplex).filter(o -> truthValue(lambda.call(o)));
        } else {
            String s = obj.toString();
            JyxalList list = JyxalList.create();
            for (char c : s.toCharArray()) {
                if (truthValue(lambda.call(Character.toString(c)))) {
                    list.add(Character.toString(c));
                }
            }
            return list;
        }
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

    public static int fromBaseDigitsAlphabet(CharSequence digits, String alphabet) {
        int result = 0;
        for (int i = 0; i < digits.length(); i++) {
            result = result * alphabet.length() + alphabet.indexOf(digits.charAt(i));
        }
        return result;
    }

    public static Object mapLambda(Lambda lambda, Object obj) {
        if (obj instanceof JyxalList jyxalList) {
            return jyxalList.map(lambda::call);
        } else if (obj instanceof BigComplex bigComplex) {
            return JyxalList.range(BigComplex.ONE, bigComplex).map(lambda::call);
        } else {
            String s = obj.toString();
            JyxalList list = JyxalList.create();
            for (char c : s.toCharArray()) {
                list.add(lambda.call(Character.toString(c)));
            }
            return list;
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

    public static Iterator<Object> replacementIterator(Iterator<Object> iterator, int index, Object replacement) {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Object next() {
                if (i++ == index) {
                    return replacement;
                } else {
                    return iterator.next();
                }
            }
        };
    }

    private static int slen(JyxalList first, JyxalList... rest) {
        int size = first.size();
        for (JyxalList list : rest) {
            size = Math.min(size, list.size());
        }
        return size;
    }

    public static List<BigInteger> toBaseDigits(BigInteger integer, BigInteger base) {
        List<BigInteger> result = new ArrayList<>();
        BigInteger remainder = integer;
        while (remainder.compareTo(base) >= 0) {
            BigInteger[] div = remainder.divideAndRemainder(base);
            result.add(div[1]);
            remainder = div[0];
        }
        result.add(remainder);
        Collections.reverse(result);
        return result;
    }

    public static String toBaseDigitsAlphabet(long integer, String alphabet) {
        StringBuilder sb = new StringBuilder();
        long remainder = integer;
        int base = alphabet.length();
        while (remainder >= base) {
            long div = remainder / base;
            sb.append(alphabet.charAt((int) (remainder % base)));
            remainder = div;
        }
        sb.append(alphabet.charAt((int) remainder));
        return sb.reverse().toString();
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

    /**
     * Unescapes a string that contains standard Java escape sequences.
     * <ul>
     * <li><strong>&#92;b &#92;f &#92;n &#92;r &#92;t &#92;" &#92;'</strong> :
     * BS, FF, NL, CR, TAB, double and single quote.</li>
     * <li><strong>&#92;X &#92;XX &#92;XXX</strong> : Octal character
     * specification (0 - 377, 0x00 - 0xFF).</li>
     * <li><strong>&#92;uXXXX</strong> : Hexadecimal based Unicode character.</li>
     * </ul>
     * <p>
     * Authored by uklimaschewski, placed in the public domain.
     *
     * @param st A string optionally containing standard java escape sequences.
     * @return The translated string.
     */
    public static String unescapeString(String st) {

        StringBuilder sb = new StringBuilder(st.length());

        for (int i = 0; i < st.length(); i++) {
            char ch = st.charAt(i);
            if (ch == '\\') {
                char nextChar = (i == st.length() - 1) ? '\\' : st
                        .charAt(i + 1);
                // Octal escape?
                if (nextChar >= '0' && nextChar <= '7') {
                    String code = "" + nextChar;
                    i++;
                    if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                            && st.charAt(i + 1) <= '7') {
                        code += st.charAt(i + 1);
                        i++;
                        if ((i < st.length() - 1) && st.charAt(i + 1) >= '0'
                                && st.charAt(i + 1) <= '7') {
                            code += st.charAt(i + 1);
                            i++;
                        }
                    }
                    sb.append((char) Integer.parseInt(code, 8));
                    continue;
                }
                switch (nextChar) {
                    case '\\' -> ch = '\\';
                    case 'b' -> ch = '\b';
                    case 'f' -> ch = '\f';
                    case 'n' -> ch = '\n';
                    case 'r' -> ch = '\r';
                    case 't' -> ch = '\t';
                    case '\"' -> ch = '\"';
                    case '\'' -> ch = '\'';

                    // Hex Unicode: u????
                    case 'u' -> {
                        if (i >= st.length() - 5) {
                            ch = 'u';
                            break;
                        }
                        int code = Integer.parseInt(
                                "" + st.charAt(i + 2) + st.charAt(i + 3)
                                        + st.charAt(i + 4) + st.charAt(i + 5), 16);
                        sb.append(Character.toChars(code));
                        i += 5;
                        continue;
                    }
                }
                i++;
            }
            sb.append(ch);
        }
        return sb.toString();
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
