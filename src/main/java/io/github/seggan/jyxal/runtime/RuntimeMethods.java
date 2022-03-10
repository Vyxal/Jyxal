package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import io.github.seggan.jyxal.runtime.math.BigComplexMath;
import io.github.seggan.jyxal.runtime.text.JsonParser;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

// All return values must be either BigComplex, JyxalList, or String.
@SuppressWarnings("UnusedReturnValue")
public final class RuntimeMethods {

    private static final LazyInit<Pattern> COMMA_PATTERN = LazyInit.regex(",");
    private static final LazyInit<Pattern> SPACE_PATTERN = LazyInit.regex(" ");
    private static final LazyInit<Pattern> PLUS_SPACE_I_PATTERN = LazyInit.regex("[+\\si]");

    private static final Map<String, Pattern> regexCache = new HashMap<>();

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

    public static Object divide(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::divide, stack);
        if (o != null) return o;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca) {
            if (b instanceof BigComplex cb) {
                return ca.divide(cb, MathContext.DECIMAL128);
            }
            JyxalList list = JyxalList.create();
            StringBuilder sb = new StringBuilder();
            BigInteger count = BigInteger.ZERO;
            BigInteger max = ca.re.toBigInteger();
            for (char c : a.toString().toCharArray()) {
                if (count.equals(max)) {
                    list.add(sb.toString());
                    sb = new StringBuilder();
                }
                sb.append(c);
                count = count.add(BigInteger.ONE);
            }
            if (sb.length() > 0) {
                list.add(sb.toString());
            }
            return list;
        } else if (b instanceof BigComplex cb) {
            JyxalList list = JyxalList.create();
            StringBuilder sb = new StringBuilder();
            BigInteger count = BigInteger.ZERO;
            BigInteger max = cb.re.toBigInteger();
            for (char c : a.toString().toCharArray()) {
                if (count.equals(max)) {
                    list.add(sb.toString());
                    sb = new StringBuilder();
                }
                sb.append(c);
                count = count.add(BigInteger.ONE);
            }
            if (sb.length() > 0) {
                list.add(sb.toString());
            }
            return list;
        } else {
            JyxalList list = JyxalList.create();
            String str = a.toString();
            String delimiter = b.toString();
            int start = 0;
            int end = str.indexOf(delimiter);
            while (end != -1) {
                list.add(str.substring(start, end));
                start = end + delimiter.length();
                end = str.indexOf(delimiter, start);
            }
            if (start < str.length()) {
                list.add(str.substring(start));
            }
            return list;
        }
    }

    public static Object doubleRepeat(Object obj) {
        if (obj instanceof JyxalList list) {
            return list.map(RuntimeMethods::doubleRepeat);
        } else if (obj instanceof BigComplex complex) {
            return complex.multiply(2);
        } else if (obj instanceof Lambda lambda) {
            return new Lambda(2, lambda.handle());
        } else {
            return obj.toString().repeat(2);
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

    public static Object ior(Object obj) {
        if (obj instanceof JyxalList list) {
            return list.map(RuntimeMethods::ior);
        } else if (obj instanceof BigComplex complex) {
            return JyxalList.range(BigComplex.ONE, complex.add(BigComplex.ONE));
        } else {
            return obj.toString().toUpperCase(Locale.ROOT);
        }
    }

    public static Object isPrime(Object obj) {
        if (obj instanceof BigComplex complex) {
            BigInteger n = complex.re.toBigInteger();
            BigDecimal bsqrt = complex.re.sqrt(MathContext.DECIMAL128);
            if (n.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) < 0
                    && n.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) > 0) {
                long l = n.longValue();
                if (l < 2) return false;
                if (l == 2 || l == 3) return true;
                if ((l & 1) == 0) return false;

                long sqrt = bsqrt.longValue();
                for (long i = 5; i <= sqrt; i += 2) {
                    if (l % i == 0) {
                        return BigComplex.valueOf(false);
                    }
                }
            } else {
                if (!n.testBit(0)) return false;

                BigInteger sqrt = bsqrt.toBigInteger();
                for (BigInteger i = BigInteger.valueOf(3); i.compareTo(sqrt) <= 0; i = i.add(BigInteger.TWO)) {
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

    public static Object joinByNothing(Object obj) {
        if (obj instanceof JyxalList list) {
            StringBuilder sb = new StringBuilder();
            for (Object item : list) {
                sb.append(item);
            }
            return sb.toString();
        } else if (obj instanceof BigComplex complex) {
            return BigComplex.valueOf(complex.abs(MathContext.DECIMAL128).compareTo(BigDecimal.ONE) <= 0);
        } else if (obj instanceof Lambda lambda) {
            BigComplex result = BigComplex.ZERO;
            while (!RuntimeHelpers.truthValue(lambda.call(result))) {
                result = result.add(BigComplex.ONE);
            }
            return result;
        } else {
            return obj.toString();
        }
    }

    public static Object jsonParse(Object obj) {
        return new JsonParser(obj.toString()).parse();
    }

    public static Object length(Object obj) {
        if (obj instanceof JyxalList list) {
            if (list.isLazy()) {
                long length = 0;
                for (Object ignored : list) {
                    length++;
                }
                return BigInteger.valueOf(length);
            } else {
                return BigComplex.valueOf(list.size());
            }
        } else {
            return BigComplex.valueOf(obj.toString().length());
        }
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

    public static Object merge(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof JyxalList jyxalList) {
            if (b instanceof JyxalList jyxalList2) {
                return jyxalList.addAllNew(jyxalList2);
            } else {
                return jyxalList.append(b);
            }
        } else if (b instanceof JyxalList jyxalList) {
            return jyxalList.addNew(0, a);
        } else {
            return a + b.toString();
        }
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

    public static Object prepend(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof JyxalList list) {
            return list.addNew(0, b);
        } else if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            return BigComplex.valueOf(new BigDecimal(cb + ca.toString()));
        } else {
            return a + b.toString();
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

    public static Object reverse(Object obj) {
        if (obj instanceof JyxalList list) {
            JyxalList newList = JyxalList.create();
            for (Object item : list) {
                newList.add(0, item);
            }
            return newList;
        } else {
            String str = obj.toString();
            StringBuilder sb = new StringBuilder();
            for (int i = str.length() - 1; i >= 0; i--) {
                sb.append(str.charAt(i));
            }
            return sb.toString();
        }
    }

    public static Object sliceUntil(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::sliceUntil, stack);
        if (o != null) return o;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca) {
            return sliceUntilImpl(b, ca.re.toBigInteger());
        } else if (b instanceof BigComplex cb) {
            return sliceUntilImpl(a, cb.re.toBigInteger());
        } else {
            Matcher matcher = regexCache.computeIfAbsent(a.toString(), Pattern::compile).matcher(b.toString());
            JyxalList list = JyxalList.create();
            while (matcher.find()) {
                list.add(matcher.group());
            }
            return list;
        }
    }

    private static Object sliceUntilImpl(Object a, BigInteger b) {
        Iterator<Object> iterator = RuntimeHelpers.iterator(a);
        return JyxalList.create(new Iterator<>() {
            private BigInteger count = BigInteger.ZERO;

            @Override
            public boolean hasNext() {
                return count.compareTo(b) < 0;
            }

            @Override
            public Object next() {
                count = count.add(BigInteger.ONE);
                return iterator.next();
            }
        });
    }

    public static Object sortByFunction(ProgramStack stack) {
        Object b = stack.pop();
        Object a = stack.pop();
        if (b instanceof Lambda lambda) {
            List<Object> list = new ArrayList<>();
            RuntimeHelpers.iterator(a).forEachRemaining(list::add);
            list.sort((o1, o2) -> {
                BigComplex r1 = sortByFunctionHelper(lambda.call(o1));
                BigComplex r2 = sortByFunctionHelper(lambda.call(o2));
                return r1.compareTo(r2);
            });
            return JyxalList.create(list);
        } else if (a instanceof BigComplex ca && b instanceof BigComplex cb) {
            return JyxalList.range(ca, cb);
        } else {
            String[] split = regexCache.computeIfAbsent(b.toString(), Pattern::compile).split(a.toString());
            return JyxalList.create((Object[]) split);
        }
    }

    private static BigComplex sortByFunctionHelper(Object obj) {
        if (obj instanceof BigComplex c) {
            return c;
        } else if (obj instanceof JyxalList list) {
            return BigComplex.valueOf(list.size());
        } else {
            return BigComplex.valueOf(obj.toString().length());
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

    public static Object subtract(ProgramStack stack) {
        Object o = RuntimeHelpers.vectorise(2, RuntimeMethods::subtract, stack);
        if (o != null) return o;
        Object b = stack.pop();
        Object a = stack.pop();
        if (a instanceof BigComplex ca) {
            if (b instanceof BigComplex cb) {
                return ca.subtract(cb);
            }
            return "-".repeat(ca.re.intValue()) + b;
        } else if (b instanceof BigComplex cb) {
            return a + "-".repeat(cb.re.intValue());
        } else {
            return a.toString().replace(b.toString(), "");
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
