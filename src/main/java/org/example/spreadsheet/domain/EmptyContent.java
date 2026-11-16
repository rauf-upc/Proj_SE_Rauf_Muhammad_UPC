package org.example.spreadsheet.domain;

/** Shared blank content, distinct from numeric zero or whitespace text. */
public enum EmptyContent implements CellContent {
    INSTANCE;

    @Override
    public String rawContent() {
        return "";
    }
}
