package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import io.github.seggan.jyxal.runtime.math.BigComplexMath;
import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuntimeMethods {

    private static final Set<Character> vowels = Set.of('a', 'e', 'i', 'o', 'u', 'y');
    private static final LazyInit<JShell> jShell = new LazyInit<>(() -> {
        JShell shell = JShell.create();
        Runtime.getRuntime().addShutdownHook(new Thread(shell::close));
        return shell;
    });
    private static final LazyInit<Pattern> NUMBER_PATTERN = new LazyInit<>(() -> Pattern.compile("\\d+(\\.\\d+)?"));
    private static final LazyInit<Pattern> LIST_PATTERN = new LazyInit<>(() -> Pattern.compile("\\[.+(?:,(.+))*]"));

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

    public static void all(ProgramStack stack) {
        Object obj = stack.pop();
        if (obj instanceof JyxalList list) {
            for (Object item : list) {
                if (!RuntimeHelpers.truthValue(item)) {
                    stack.push(false);
                    return;
                }
            }
            stack.push(true);
        } else if (obj instanceof String string) {
            for (char c : string.toCharArray()) {
                if (!vowels.contains(c)) {
                    stack.push(false);
                    return;
                }
            }
            stack.push(true);
        }

        stack.push(true);
    }

    public static void chrOrd(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(1, RuntimeMethods::chrOrd, stack)) return;
        Object obj = stack.pop();
        if (obj instanceof BigComplex complex) {
            stack.push(Character.toString(complex.re.intValue()));
        } else {
            String str = obj.toString();
            if (str.length() == 1) {
                // Due to overload resolution, it will push the int value
                stack.push(str.charAt(0));
            } else {
                JyxalList list = JyxalList.create();
                for (char c : str.toCharArray()) {
                    list.add((int) c);
                }
                stack.push(list);
            }
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

    private static JyxalList deepCopy(JyxalList list) {
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

    public static void duplicate(ProgramStack stack) {
        Object obj = Objects.requireNonNull(stack.peek());
        if (obj instanceof JyxalList jyxalList) {
            // deep copy
            stack.push(deepCopy(jyxalList));
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

    public static void greaterThan(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::greaterThan, stack)) return;
        compare(stack, i -> i > 0);
    }

    public static void greaterThanOrEqual(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(2, RuntimeMethods::greaterThanOrEqual, stack)) return;
        compare(stack, i -> i >= 0);
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
                stack.push(repeatCharacters(b.toString(), ca.re.intValue()));
            }
        } else if (b instanceof BigComplex cb) {
            stack.push(repeatCharacters(a.toString(), cb.re.intValue()));
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

    private static void pushExpr(ProgramStack stack, String expr) {
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

    private static String repeatCharacters(String str, int times) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            sb.append(String.valueOf(c).repeat(Math.max(0, times)));
        }

        return sb.toString();
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
            stack.push(deepCopy(jyxalList));
            stack.push(deepCopy(jyxalList));
        } else {
            stack.push(obj);
            stack.push(obj);
        }
    }

    public static void twoPow(ProgramStack stack) {
        if (RuntimeHelpers.vectorise(1, RuntimeMethods::twoPow, stack)) return;
        Object obj = stack.pop();
        if (obj instanceof BigComplex complex) {
            stack.push(BigComplexMath.pow(BigComplex.valueOf(2), complex, MathContext.DECIMAL128));
        } else {
            String str = obj.toString();
            for (SnippetEvent e : jShell.get().eval(jShell.get().sourceCodeAnalysis().analyzeCompletion(str).source())) {
                if (e.status() == Snippet.Status.VALID) {
                    pushExpr(stack, e.value());
                } else {
                    throw new RuntimeException(e.toString());
                }
            }
        }
    }
}
