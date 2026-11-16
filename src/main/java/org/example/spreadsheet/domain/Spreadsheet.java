package org.example.spreadsheet.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Sparse cell aggregate. Has no CLI, file I/O, parser, or evaluation dependency. */
public final class Spreadsheet {
    private final Map<CellAddress, Cell> cells = new HashMap<>();

    /** Sets or replaces raw content; an empty string clears the addressed cell. */
    public void setContent(CellAddress address, String rawContent) {
        Objects.requireNonNull(address, "address");
        CellContent content = CellContent.fromRaw(rawContent);
        if (content == EmptyContent.INSTANCE) {
            cells.remove(address);
        } else {
            cells.put(address, new Cell(address, content));
        }
    }

    /** Reading an untouched address returns a blank cell without changing this sheet. */
    public Cell getCell(CellAddress address) {
        Objects.requireNonNull(address, "address");
        Cell cell = cells.get(address);
        return cell != null ? cell : new Cell(address, EmptyContent.INSTANCE);
    }

    public CellContent getContent(CellAddress address) {
        return getCell(address).content();
    }

    public String getRawContent(CellAddress address) {
        return getContent(address).rawContent();
    }

    /**
     * Returns an immutable snapshot of non-empty cells. Iteration order is unspecified;
     * future persistence must explicitly order rows and columns.
     */
    public Map<CellAddress, Cell> getCells() {
        return Map.copyOf(cells);
    }
}
