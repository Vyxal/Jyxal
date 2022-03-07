package io.github.seggan.jyxal.runtime.text;

import io.github.seggan.jyxal.runtime.LazyInit;
import io.github.seggan.jyxal.runtime.RuntimeHelpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Compression related stuff
 */
public class Compression {

    public static final String CODEPAGE = "\u03BB\u019B\u00AC\u2227\u27D1\u2228\u27C7\u00F7\u00D7\u00AB\n\u00BB\u00B0\u2022\u00DF\u2020\u20AC\u00BD\u2206\u00F8\u2194\u00A2\u2310\u00E6\u0280\u0281\u027E\u027D\u00DE\u0188\u221E\u00A8 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]`^_abcdefghijklmnopqrstuvwxyz{|}~\u2191\u2193\u2234\u2235\u203A\u2039\u2237\u00A4\u00F0\u2192\u2190\u03B2\u03C4\u0227\u1E03\u010B\u1E0B\u0117\u1E1F\u0121\u1E23\u1E2D\u0140\u1E41\u1E45\u022F\u1E57\u1E59\u1E61\u1E6B\u1E87\u1E8B\u1E8F\u017C\u221A\u27E8\u27E9\u201B\u2080\u2081\u2082\u2083\u2084\u2085\u2086\u2087\u2088\u00B6\u204B\u00A7\u03B5\u00A1\u2211\u00A6\u2248\u00B5\u0226\u1E02\u010A\u1E0A\u0116\u1E1E\u0120\u1E22\u0130\u013F\u1E40\u1E44\u022E\u1E56\u1E58\u1E60\u1E6A\u1E86\u1E8A\u1E8E\u017B\u208C\u208D\u2070\u00B9\u00B2\u2207\u2308\u230A\u00AF\u00B1\u20B4\u2026\u25A1\u21B3\u21B2\u22CF\u22CE\uA60D\uA71D\u2105\u2264\u2265\u2260\u207C\u0192\u0256\u222A\u2229\u228D\u00A3\u00A5\u21E7\u21E9\u01CD\u01CE\u01CF\u01D0\u01D1\u01D2\u01D3\u01D4\u207D\u2021\u226C\u207A\u21B5\u215B\u00BC\u00BE\u03A0\u201E\u201F";
    public static final String COMPRESSION_CODEPAGE = "\u03BB\u019B\u00AC\u2227\u27D1\u2228\u27C7\u00F7\u00D7\u00AB\u00BB\u00B0\u2022\u00DF\u2020\u20AC\u00BD\u2206\u00F8\u2194\u00A2\u2310\u00E6\u0280\u0281\u027E\u027D\u00DE\u0188\u221E\u00A8\u2191\u2193\u2234\u2235\u203A\u2039\u2237\u00A4\u00F0\u2192\u2190\u03B2\u03C4\u0227\u1E03\u010B\u1E0B\u0117\u1E1F\u0121\u1E23\u1E2D\u0140\u1E41\u1E45\u022F\u1E57\u1E59\u1E61\u1E6B\u1E87\u1E8B\u1E8F\u017C\u221A\u27E8\u27E9\u201B\u2080\u2081\u2082\u2083\u2084\u2085\u2086\u2087\u2088\u00B6\u204B\u00A7\u03B5\u00A1\u2211\u00A6\u2248\u00B5\u0226\u1E02\u010A\u1E0A\u0116\u1E1E\u0120\u1E22\u0130\u013F\u1E40\u1E44\u022E\u1E56\u1E58\u1E60\u1E6A\u1E86\u1E8A\u1E8E\u017B\u208C\u208D\u2070\u00B9\u00B2\u2207\u2308\u230A\u00AF\u00B1\u20B4\u2026\u25A1\u21B3\u21B2\u22CF\u22CE\uA60D\uA71D\u2105\u2264\u2265\u2260\u207C\u0192\u0256\u222A\u2229\u228D\u00A3\u00A5\u21E7\u21E9\u01CD\u01CE\u01CF\u01D0\u01D1\u01D2\u01D3\u01D4\u207D\u2021\u226C\u207A\u21B5\u215B\u00BC\u00BE\u03A0\u201E\u201F";

    private static final LazyInit<String[]> longDict = new LazyInit<>(() -> {
        String s;
        try (InputStream in = Compression.class.getClassLoader().getResourceAsStream("dictLong.txt")) {
            s = new String(in.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s.replace("\r", "").split("\n");
    });

    private static final LazyInit<String[]> shortDict = new LazyInit<>(() -> {
        String s;
        try (InputStream in = Compression.class.getClassLoader().getResourceAsStream("dictShort.txt")) {
            s = new String(in.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s.replace("\r", "").split("\n");
    });

    private Compression() {
    }

    public static String decompress(String str) {
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        Deque<Character> chars = new ArrayDeque<>(str.length());
        for (char c : str.toCharArray()) {
            chars.add(c);
        }
        while (!chars.isEmpty()) {
            char c = chars.removeFirst();
            if (COMPRESSION_CODEPAGE.indexOf(c) != -1) {
                temp.append(c);
                if (temp.length() == 2) {
                    int index = RuntimeHelpers.fromBaseDigitsAlphabet(temp, COMPRESSION_CODEPAGE);
                    String[] dict = longDict.get();
                    if (index < dict.length) {
                        sb.append(dict[index]);
                    }
                    temp.setLength(0);
                }
            } else {
                if (temp.length() > 0) {
                    int index = COMPRESSION_CODEPAGE.indexOf(temp.toString());
                    String[] dict = shortDict.get();
                    if (index < dict.length) {
                        sb.append(dict[index]);
                    }
                    temp.setLength(0);
                    if (c == ' ') {
                        continue;
                    }
                }
                sb.append(c);
            }
        }
        if (temp.length() > 0) {
            int index = RuntimeHelpers.fromBaseDigitsAlphabet(temp, COMPRESSION_CODEPAGE);
            String[] dict = shortDict.get();
            if (index < dict.length) {
                sb.append(dict[index]);
            }
        }
        return sb.toString();
    }
}
