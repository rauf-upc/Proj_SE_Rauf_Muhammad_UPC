package org.example.spreadsheet.domain;

import java.util.Objects;

/** Non-empty text that is neither a supported numeric literal nor a leading '=' formula. */
public record TextContent(String rawContent) implements CellContent {
    public TextContent {
        Objects.requireNonNull(rawContent, "rawContent");
        if (rawContent.isEmpty() || rawContent.startsWith("=") || NumericContent.isNumeric(rawContent)) {
            throw new IllegalArgumentException("Text must not be blank, numeric, or formula content: " + rawContent);
        }
    }
}
