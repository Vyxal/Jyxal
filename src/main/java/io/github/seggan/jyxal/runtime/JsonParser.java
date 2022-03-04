package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.math.BigDecimal;

public class JsonParser {

    private final String json;
    private int index;

    public JsonParser(String json) {
        this.json = json;
    }

    // must be String, BigComplex, or JyxalList
    public Object parse() {
        skipWhitespace();
        checkEnd();
        Object result = parseValue();
        index = 0;
        return result;
    }

    private Object parseValue() {
        return switch (json.charAt(index)) {
            case '"' -> parseString();
            case '{' -> parseObject();
            case '[' -> parseArray();
            case 't' -> parseTrue();
            case 'f' -> parseFalse();
            case 'n' -> parseNull();
            case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> parseNumber();
            default -> throw new RuntimeException("Unexpected character %s at index %d in string %s".formatted(json.charAt(index), index, json));
        };
    }

    private Object parseTrue() {
        index += 4;
        return BigComplex.valueOf(true);
    }

    private Object parseFalse() {
        index += 5;
        return BigComplex.valueOf(false);
    }

    private Object parseNull() {
        index += 4;
        return BigComplex.valueOf(false);
    }

    private Object parseNumber() {
        StringBuilder sb = new StringBuilder();
        if (json.charAt(index) == '-') {
            sb.append(json.charAt(index));
            index++;
        }
        while (index < json.length() && (Character.isDigit(json.charAt(index)) || json.charAt(index) == '.')) {
            sb.append(json.charAt(index));
            index++;
        }
        return BigComplex.valueOf(new BigDecimal(sb.toString()));
    }

    private Object parseArray() {
        index++;
        skipWhitespace();
        JyxalList result = JyxalList.create();
        if (json.charAt(index) == ']') {
            index++;
            return result;
        }
        while (true) {
            checkEnd();
            result.add(parseValue());
            skipWhitespace();
            checkEnd();
            if (json.charAt(index) == ']') {
                index++;
                break;
            }
            if (json.charAt(index) != ',') {
                throw new RuntimeException("Expected ',' or ']' at index %d in string %s".formatted(index, json));
            }
            index++;
            skipWhitespace();
        }
        return result;
    }

    private JyxalList parseObject() {
        index++;
        skipWhitespace();
        JyxalList result = JyxalList.create();
        if (json.charAt(index) == '}') {
            index++;
            return result;
        }
        while (true) {
            checkEnd();
            String key = parseString();
            skipWhitespace();
            checkEnd();
            if (json.charAt(index) != ':') {
                throw new RuntimeException("Expected ':' at index %d in string %s".formatted(index, json));
            }
            index++;
            skipWhitespace();
            checkEnd();
            result.add(JyxalList.create(key, parseValue()));
            skipWhitespace();
            checkEnd();
            if (json.charAt(index) == '}') {
                index++;
                break;
            }
            if (json.charAt(index) != ',') {
                throw new RuntimeException("Expected ',' or '}' at index %d in string %s".formatted(index, json));
            }
            index++;
            skipWhitespace();
        }
        return result;
    }

    private String parseString() {
        index++;
        StringBuilder sb = new StringBuilder();
        char c = json.charAt(index);
        while (c != '"') {
            sb.append(c);
            index++;
            checkEnd();
            c = json.charAt(index);
        }
        index++;
        return RuntimeHelpers.unescapeString(sb.toString());
    }

    private void checkEnd() {
        if (index == json.length()) {
            throw new RuntimeException("Unexpected end of JSON string '%s'".formatted(json));
        }
    }

    private void skipWhitespace() {
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
    }
}
