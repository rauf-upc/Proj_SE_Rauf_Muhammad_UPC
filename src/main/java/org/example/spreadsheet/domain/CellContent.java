package org.example.spreadsheet.domain;

/** Immutable cell source; evaluated values belong to a later, separate component. */
public sealed interface CellContent permits EmptyContent, NumericContent, TextContent, FormulaContent {
    String rawContent();

    /** Classifies raw content without trimming, formula parsing, or calculation. */
    static CellContent fromRaw(String rawContent) {
        return CellContentFactory.fromRaw(rawContent);
    }
}
