package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import io.github.seggan.jyxal.runtime.math.BigComplexMath;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

// All return values must be either BigComplex, JyxalList, or String.
@SuppressWarnings("UnusedReturnValue")
public final class RuntimeMethods {

    private static final LazyInit<Pattern> COMMA_PATTERN = LazyInit.regex(",");
    private static final LazyInit<Pattern> SPACE_PATTERN = LazyInit.regex(" ");
    private static final LazyInit<Pattern> PLUS_SPACE_I_PATTERN = LazyInit.regex("[+\\si]");

    private RuntimeMethods() {
    }

    public static Object add(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        return addImpl(a, b);
    }

    private static Object addImpl(Object a, Object b) {
        if (a instanceof JyxalList listA) {
            if (b instanceof JyxalList listB) {
                JyxalList result = JyxalList.create();
                for (int i = 0, size = Math.min(listA.size(), listB.size()); i < size; i++) {
                    result.add(addImpl(listA.get(i), listB.get(i)));
                }
                return result;
            }
            return listA.map(item -> addImpl(item, b));
        } else if (b instanceof JyxalList listB) {
            return listB.map(item -> addImpl(a, item));
        }

        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            return ca.add(cb);
        } else {
            return a + b.toString();
        }
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

    private static Object compare(ProgramStack stack, IntPredicate predicate) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            return BigComplex.valueOf(predicate.test(ca.compareTo(cb)));
        } else {
            return BigComplex.valueOf(predicate.test(a.toString().compareTo(b.toString())));
        }
    }

    public static Object complement(Object obj) {
        if (obj instanceof BigComplex complex) {
            return BigComplex.ONE.subtract(complex);
        } else {
            return JyxalList.create((Object[]) COMMA_PATTERN.get().split(obj.toString()));
        }
    }

    public static Object duplicate(ProgramStack stack) {
        Object obj = Objects.requireNonNull(stack.peek());
        if (obj instanceof JyxalList jyxalList) {
            // deep copy
            return RuntimeHelpers.deepCopy(jyxalList);
        } else {
            return obj;
        }
    }

    public static Object equals(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::equals, stack);
        if (o != null) return o;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            return BigComplex.valueOf(ca.equals(cb));
        } else {
            return BigComplex.valueOf(a.toString().equals(b.toString()));
        }
    }

    public static Object flatten(Object obj) {
        if (obj instanceof JyxalList list) {
            return flattenImpl(list);
        } else {
            JyxalList list = JyxalList.create();
            for (char c : obj.toString().toCharArray()) {
                list.add(Character.toString(c));
            }
            return list;
        }
    }

    private static JyxalList flattenImpl(JyxalList list) {
        JyxalList newList = JyxalList.create();
        for (Object item : list) {
            if (item instanceof JyxalList subList) {
                newList.addAll(flattenImpl(subList));
            } else {
                newList.add(item);
            }
        }
        return newList;
    }

    public static Object functionCall(ProgramStack stack) {
        Object obj = stack.pop();
        if (obj instanceof Lambda lambda) {
            return lambda.call(stack);
        } else if (obj instanceof JyxalList list) {
            return list.map(o -> BigComplex.valueOf(!RuntimeHelpers.truthValue(o)));
        } else if (obj instanceof BigComplex complex) {
            return RuntimeHelpers.primeFactors(complex, HashSet::new).size();
        } else {
            return RuntimeHelpers.exec(obj.toString());
        }
    }

    public static Object getRequest(Object obj) throws IOException {
        String url = obj.toString();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 Jyxal");
        connection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        connection.setInstanceFollowRedirects(true);
        connection.connect();
        int code = connection.getResponseCode();
        if (code / 100 == 3) {
            String location = connection.getHeaderField("Location");
            if (location != null) {
                return RuntimeMethods.getRequest(location);
            } else {
                throw new IOException("Redirect without location");
            }
        } else if (code / 100 != 2) {
            return BigComplex.valueOf(code);
        }
        byte[] response;
        try (InputStream inputStream = connection.getInputStream()) {
            response = inputStream.readAllBytes();
        }
        if (Objects.equals(connection.getContentEncoding(), "gzip")) {
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(response))) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return new String(response, StandardCharsets.UTF_8);
            }
        }
        return new String(response, StandardCharsets.UTF_8);
    }

    public static Object greaterThan(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::greaterThan, stack);
        if (o != null) return o;
        return compare(stack, i -> i > 0);
    }

    public static Object greaterThanOrEqual(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::greaterThanOrEqual, stack);
        if (o != null) return o;
        return compare(stack, i -> i >= 0);
    }

    public static Object halve(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(1, RuntimeMethods::halve, stack);
        if (o != null) return o;
        Object obj = stack.pop();
        if (obj instanceof BigComplex complex) {
            return complex.divide(BigComplex.TWO, MathContext.DECIMAL128);
        } else {
            String str = obj.toString();
            int limit = str.length() / 2 + 1;
            stack.push(str.substring(0, limit));
            return str.substring(limit);
        }
    }

    public static Object head(Object obj) {
        if (obj instanceof JyxalList list) {
            return list.get(0);
        } else {
            if (obj.toString().length() > 0) {
                return obj.toString().substring(0, 1);
            } else {
                return BigComplex.ZERO;
            }
        }
    }

    public static Object increment(Object obj) {
        if (obj instanceof BigComplex c) {
            return c.add(BigComplex.ONE);
        } else {
            return SPACE_PATTERN.get().matcher(obj.toString()).replaceAll("0");
        }
    }

    public static Object infinitePrimes() {
        return JyxalList.createInf(new Supplier<>() {
            BigInteger next = BigInteger.ONE;

            @Override
            public Object get() {
                next = next.nextProbablePrime();
                return next;
            }
        });
    }

    public static Object infiniteReplace(ProgramStack stack) {
        Object c = stack.pop();
        Object b = stack.pop();
        Object a = stack.pop();

        if (a instanceof JyxalList list) {
            JyxalList prev = list;
            do {
                list = list.map(o -> o.equals(b) ? c : o);
                if (list.equals(prev)) break;
                prev = list;
            } while (true);

            return list;
        } else {
            String aString = a.toString();
            String bString = b.toString();
            String cString = c.toString();

            String prev = aString;
            do {
                aString = aString.replace(bString, cString);
                if (a.equals(prev)) break;
                prev = aString;
            } while (true);

            return aString;
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

    public static Object itemSplit(ProgramStack stack) {
        Object obj = stack.pop();
        if (obj instanceof BigComplex complex) {
            obj = complex.re.toBigInteger().toString();
        }
        if (obj instanceof JyxalList list) {
            int listSize = list.size() - 1;
            for (int i = 0; i < listSize; i++) {
                Object item = list.get(i);
                stack.push(item);
            }
            return list.get(listSize);
        } else {
            char[] charArray = obj.toString().toCharArray();
            for (int i = 0, charArrayLength = charArray.length - 1; i < charArrayLength; i++) {
                char c = charArray[i];
                stack.push(Character.toString(c));
            }
            return Character.toString(charArray[charArray.length - 1]);
        }
    }

    public static Object izr(Object obj) {
        if (obj instanceof BigComplex complex) {
            return JyxalList.range(BigComplex.ZERO, complex.add(BigComplex.ONE));
        } else {
            JyxalList list = JyxalList.create();
            for (char c : obj.toString().toCharArray()) {
                list.add(BigComplex.valueOf(Character.isAlphabetic(c)));
            }
            return list;
        }
    }

    public static Object jsonParse(Object obj) {
        return new JsonParser(obj.toString()).parse();
    }

    public static Object lessThan(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::lessThan, stack);
        if (o != null) return o;
        return compare(stack, i -> i < 0);
    }

    public static Object lessThanOrEqual(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::lessThanOrEqual, stack);
        if (o != null) return o;
        return compare(stack, i -> i <= 0);
    }

    public static Object logicalAnd(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (RuntimeHelpers.truthValue(a)) {
            if (RuntimeHelpers.truthValue(b)) {
                return a;
            } else {
                return b;
            }
        } else {
            return a;
        }
    }

    public static Object logicalOr(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (RuntimeHelpers.truthValue(a)) {
            return a;
        } else {
            if (RuntimeHelpers.truthValue(b)) {
                return b;
            } else {
                return a;
            }
        }
    }

    public static Object mapGetSet(ProgramStack stack) {
        Object map = stack.pop();
        Object key = stack.pop();
        if (key instanceof JyxalList list) {
            // set
            key = stack.pop();
            int i = 0;
            for (Object o : list) {
                if (o instanceof JyxalList pair && pair.size() >= 2 && pair.get(0).equals(key)) {
                    return JyxalList.create(RuntimeHelpers.replacementIterator(
                            list.iterator(),
                            i,
                            JyxalList.create(pair.get(0), map)
                    ));
                }
                i++;
            }
            JyxalList newList = JyxalList.create(list);
            newList.add(JyxalList.create(key, map));
            return newList;
        }
        for (Object o : (JyxalList) map) {
            if (o instanceof JyxalList pair && pair.size() >= 2 && pair.get(0).equals(key)) {
                if (pair.size() == 2) {
                    return pair.get(1);
                } else {
                    Iterator<Object> iterator = pair.iterator();
                    iterator.next();
                    return JyxalList.create(iterator);
                }
            }
        }
        return BigComplex.ZERO;
    }

    public static Object moduloFormat(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::multiCommand, stack);
        if (o != null) return o;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            // BigComplex has no mod method...
            if (cb.isReal()) {
                return BigComplex.valueOf(ca.re.remainder(cb.re), ca.im.remainder(cb.re));
            } else {
                throw new RuntimeException("Can't modulo complex numbers with non-real numbers");
            }
        } else {
            if (a instanceof BigComplex) {
                return b.toString().replace("%", a.toString());
            } else {
                return a.toString().replace("%", b.toString());
            }
        }
    }

    public static Object multiCommand(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::multiCommand, stack);
        if (o != null) return o;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca) {
            if (b instanceof BigComplex cb) {
                // complex numbers go brr
                // going off of that fact that ln(x)/ln(a) = loga(x)
                // and ln(x) = ln(|x|) + i * arg(x)
                BigComplex top = BigComplexMath.log(cb, MathContext.DECIMAL128);
                BigComplex bottom = BigComplexMath.log(ca, MathContext.DECIMAL128);
                return top.divide(bottom, MathContext.DECIMAL128);
            } else {
                return RuntimeHelpers.repeatCharacters(b.toString(), ca.re.intValue());
            }
        } else if (b instanceof BigComplex cb) {
            return RuntimeHelpers.repeatCharacters(a.toString(), cb.re.intValue());
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

            return sb.toString();
        }
    }

    public static Object multiply(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::multiply, stack);
        if (o != null) return o;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca) {
            if (b instanceof BigComplex cb) {
                return ca.multiply(cb);
            } else if (b instanceof Lambda lambda) {
                return new Lambda(ca.re.intValue(), lambda.handle());
            }
            return b.toString().repeat(ca.re.intValue());
        } else if (a instanceof Lambda lambda && b instanceof BigComplex cb) {
            return new Lambda(cb.re.intValue(), lambda.handle());
        } else {
            String aString = a.toString();
            if (b instanceof BigComplex cb) {
                return aString.repeat(cb.re.intValue());
            }

            String bString = b.toString();
            StringBuilder sb = new StringBuilder();
            for (char c : bString.toCharArray()) {
                int index = aString.indexOf(c);
                if (index >= 0) {
                    sb.append(aString.charAt((index + 1) % aString.length()));
                } else {
                    sb.append(c);
                }
            }

            return sb.toString();
        }
    }

    public static void printToFile(ProgramStack stack) throws IOException {
        try (OutputStream os = new FileOutputStream("test.out")) {
            while (!stack.isEmpty()) {
                os.write(stack.pop().toString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public static Object removeAtIndex(ProgramStack stack) {
        Object a = stack.pop();
        Object b = stack.pop();
        if (a instanceof BigComplex ca) {
            if (b instanceof JyxalList list) {
                return list.removeAtIndex(ca.re.toBigInteger());
            }
            String str = b.toString();
            int index = ca.re.intValue();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                if (i != index) {
                    sb.append(str.charAt(i));
                }
            }
            return sb.toString();
        } else if (b instanceof BigComplex cb) {
            if (a instanceof JyxalList list) {
                return list.removeAtIndex(cb.re.toBigInteger());
            }
            String str = a.toString();
            int index = cb.re.intValue();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                if (i != index) {
                    sb.append(str.charAt(i));
                }
            }
            return sb.toString();
        } else {
            throw new IllegalArgumentException("%s, %s".formatted(a, b));
        }
    }

    public static Object splitOn(ProgramStack stack) {
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
            return superList;
        } else {
            return JyxalList.create((Object[]) a.toString().split(b.toString()));
        }
    }

    public static Object sum(Object obj) {
        if (obj instanceof JyxalList list) {
            Object sum = BigComplex.ZERO;
            for (Object item : list) {
                sum = addImpl(sum, item);
            }
            return sum;
        } else if (obj instanceof BigComplex) {
            char[] chars = PLUS_SPACE_I_PATTERN.get().matcher(obj.toString()).replaceAll("").toCharArray();
            long sum = 0;
            for (char c : chars) {
                sum += c - 48;
            }
            return BigComplex.valueOf(sum);
        } else {
            long sum = 0;
            for (char c : obj.toString().toCharArray()) {
                sum += c;
            }
            return BigComplex.valueOf(sum);
        }
    }

    public static Object tail(Object obj) {
        if (obj instanceof JyxalList list) {
            if (list.isLazy()) {
                Iterator<Object> iterator = list.iterator();
                Object last = BigComplex.ZERO;
                while (iterator.hasNext()) {
                    last = iterator.next();
                }
                return last;
            } else {
                return list.get(list.size() - 1);
            }
        } else {
            String s = obj.toString();
            if (s.length() == 0) {
                return BigComplex.ZERO;
            } else {
                return s.charAt(s.length() - 1);
            }
        }
    }

    public static Object triplicate(ProgramStack stack) {
        Object obj = Objects.requireNonNull(stack.peek());
        if (obj instanceof JyxalList jyxalList) {
            // deep copy
            stack.push(RuntimeHelpers.deepCopy(jyxalList));
            return RuntimeHelpers.deepCopy(jyxalList);
        } else {
            stack.push(obj);
            return obj;
        }
    }

    public static Object twoPow(Object obj) {
        if (obj instanceof BigComplex complex) {
            return BigComplexMath.pow(BigComplex.TWO, complex, MathContext.DECIMAL128);
        } else {
            return RuntimeHelpers.exec(obj.toString());
        }
    }

    public static Object vectorise(Object obj, MethodHandle handle) throws Throwable {
        if (obj instanceof JyxalList list) {
            JyxalList result = JyxalList.create();
            for (Object item : list) {
                result.add(vectorise(item, handle));
            }
            return result;
        }

        return handle.invoke(obj);
    }
}
