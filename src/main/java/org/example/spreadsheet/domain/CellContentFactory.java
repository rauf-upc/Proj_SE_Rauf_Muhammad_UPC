package org.example.spreadsheet.domain;

import java.util.Objects;

/** Internal classification policy; this does not parse expressions. */
final class CellContentFactory {
    private CellContentFactory() {
    }

    static CellContent fromRaw(String rawContent) {
        Objects.requireNonNull(rawContent, "rawContent");
        if (rawContent.isEmpty()) {
            return EmptyContent.INSTANCE;
        }
        if (rawContent.startsWith("=")) {
            return new FormulaContent(rawContent);
        }
        if (NumericContent.isNumeric(rawContent)) {
            return new NumericContent(rawContent);
        }
        return new TextContent(rawContent);
    }
}
