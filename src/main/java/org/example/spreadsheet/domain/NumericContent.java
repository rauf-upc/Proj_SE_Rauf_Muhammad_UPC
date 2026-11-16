package org.example.spreadsheet.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/** A signed dot-decimal literal whose exact spelling is retained, not evaluated. */
public record NumericContent(String rawContent) implements CellContent {
    private static final Pattern NUMBER = Pattern.compile("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)");

    public NumericContent {
        Objects.requireNonNull(rawContent, "rawContent");
        if (!isNumeric(rawContent)) {
            throw new IllegalArgumentException("Expected a signed dot-decimal numeric literal: " + rawContent);
        }
    }

    static boolean isNumeric(String rawContent) {
        return NUMBER.matcher(rawContent).matches();
    }
}
