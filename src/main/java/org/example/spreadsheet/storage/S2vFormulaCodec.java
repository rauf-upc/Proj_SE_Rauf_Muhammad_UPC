package org.example.spreadsheet.storage;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * File delimiter conversion only. Does not parse expressions, validate their grammar,
 * resolve function names, build nodes, or calculate anything.
 */
final class S2vFormulaCodec {
    private S2vFormulaCodec() {
    }

    static String encode(String raw) throws S2vFormatException {
        return convert(raw, true);
    }

    static String decode(String stored) throws S2vFormatException {
        return convert(stored, false);
    }

    private static String convert(String content, boolean saving) throws S2vFormatException {
        if (!content.startsWith("=")) {
            return content;
        }
        boolean hasSeparator = content.indexOf(';') >= 0 || content.indexOf(',') >= 0;
        if (hasSeparator && (content.indexOf('"') >= 0 || content.indexOf('\'') >= 0)) {
            throw new S2vFormatException("Quoted formula arguments with separators have no specified S2V escaping rule");
        }

        Deque<Boolean> functionParentheses = new ArrayDeque<>();
        StringBuilder result = new StringBuilder(content.length());
        for (int i = 0; i < content.length(); i++) {
            char character = content.charAt(i);
            if (character == '(') {
                functionParentheses.push(followsFunctionName(content, i));
            } else if (character == ')' && !functionParentheses.isEmpty()) {
                functionParentheses.pop();
            }
            boolean inArguments = !functionParentheses.isEmpty() && functionParentheses.peek();
            if (saving && character == ';') {
                if (!inArguments) {
                    throw new S2vFormatException("Semicolon outside a function argument list cannot be stored in one S2V cell");
                }
                character = ',';
            } else if (character == ',' && inArguments) {
                if (saving) {
                    throw new S2vFormatException("Use ';' for formula arguments; a literal argument comma is ambiguous in S2V");
                }
                character = ';';
            }
            result.append(character);
        }
        return result.toString();
    }

    /** Recognizes only a name before '(' so grouping parentheses are not argument lists. */
    private static boolean followsFunctionName(String content, int openingParenthesis) {
        int end = openingParenthesis;
        while (end > 0 && Character.isWhitespace(content.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (start > 0 && isNameCharacter(content.charAt(start - 1))) {
            start--;
        }
        return start < end && (isLetter(content.charAt(start)) || content.charAt(start) == '_');
    }

    private static boolean isNameCharacter(char character) {
        return isLetter(character) || character == '_' || character >= '0' && character <= '9';
    }

    private static boolean isLetter(char character) {
        return character >= 'A' && character <= 'Z' || character >= 'a' && character <= 'z';
    }
}
