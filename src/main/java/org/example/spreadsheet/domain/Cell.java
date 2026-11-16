package org.example.spreadsheet.domain;

import java.util.Objects;

/** Immutable cell snapshot; edits replace the cell through its spreadsheet. */
public record Cell(CellAddress address, CellContent content) {
    public Cell {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(content, "content");
    }
}
