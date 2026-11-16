package org.example.spreadsheet.domain;

import java.util.Objects;

/** Opaque formula source, including its '=' prefix; no expression validation occurs. */
public record FormulaContent(String rawContent) implements CellContent {
    public FormulaContent {
        Objects.requireNonNull(rawContent, "rawContent");
        if (!rawContent.startsWith("=")) {
            throw new IllegalArgumentException("Formula content must begin with '=': " + rawContent);
        }
    }
}
