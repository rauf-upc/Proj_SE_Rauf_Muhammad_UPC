package org.example.spreadsheet.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpreadsheetTest {
    private final Spreadsheet sheet = new Spreadsheet();
    private final CellAddress a1 = CellAddress.parse("A1");

    @Test
    void untouchedCellsAreEmptyAndReadsDoNotPopulateTheSheet() {
        assertEquals("", sheet.getRawContent(a1));
        assertSame(EmptyContent.INSTANCE, sheet.getContent(a1));
        assertEquals(new Cell(a1, EmptyContent.INSTANCE), sheet.getCell(a1));
        assertTrue(sheet.getCells().isEmpty());
    }

    @Test
    void canSetReplaceAndRetrieveEveryContentCategory() {
        sheet.setContent(a1, "001.20");
        assertInstanceOf(NumericContent.class, sheet.getContent(a1));
        assertEquals("001.20", sheet.getRawContent(a1));

        sheet.setContent(a1, "  TOTAL català  ");
        assertInstanceOf(TextContent.class, sheet.getContent(a1));
        assertEquals("  TOTAL català  ", sheet.getRawContent(a1));

        sheet.setContent(a1, "= 1 + 2");
        assertInstanceOf(FormulaContent.class, sheet.getContent(a1));
        assertEquals("= 1 + 2", sheet.getRawContent(a1));
        assertEquals(1, sheet.getCells().size());
    }

    @Test
    void clearingRemovesOnlyTheSelectedCellAndZeroIsNotBlank() {
        CellAddress b5 = CellAddress.parse("B5");
        sheet.setContent(a1, "0");
        sheet.setContent(b5, "retained");
        assertInstanceOf(NumericContent.class, sheet.getContent(a1));
        sheet.setContent(a1, "");
        assertSame(EmptyContent.INSTANCE, sheet.getContent(a1));
        assertFalse(sheet.getCells().containsKey(a1));
        assertEquals("retained", sheet.getRawContent(b5));
        sheet.setContent(CellAddress.parse("AA1"), "");
        assertEquals(1, sheet.getCells().size());
    }

    @Test
    void cellsAndSeparateSpreadsheetsAreIndependent() {
        CellAddress aa1 = CellAddress.parse("AA1");
        sheet.setContent(a1, "first");
        sheet.setContent(aa1, "second");
        sheet.setContent(CellAddress.parse("A1"), "changed");
        assertEquals("changed", sheet.getRawContent(a1));
        assertEquals("second", sheet.getRawContent(aa1));
        assertEquals("", new Spreadsheet().getRawContent(a1));
    }

    @Test
    void invalidInputsLeavePreviousStateUnchanged() {
        sheet.setContent(a1, "previous");
        Map<CellAddress, Cell> before = sheet.getCells();
        assertThrows(NullPointerException.class, () -> sheet.setContent(a1, null));
        assertThrows(NullPointerException.class, () -> sheet.setContent(null, "replacement"));
        assertThrows(IllegalArgumentException.class,
                () -> sheet.setContent(CellAddress.parse("A0"), "replacement"));
        assertThrows(NullPointerException.class, () -> sheet.getCell(null));
        assertThrows(NullPointerException.class, () -> sheet.getContent(null));
        assertThrows(NullPointerException.class, () -> sheet.getRawContent(null));
        assertEquals(before, sheet.getCells());
        assertEquals("previous", sheet.getRawContent(a1));
    }

    @Test
    void snapshotsAreImmutableAndDoNotChangeAfterAnEdit() {
        sheet.setContent(a1, "original");
        Cell cell = sheet.getCell(a1);
        Map<CellAddress, Cell> snapshot = sheet.getCells();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.put(CellAddress.parse("B1"), cell));
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.entrySet().iterator().next().setValue(new Cell(a1, new TextContent("other"))));

        sheet.setContent(a1, "replacement");
        assertEquals("original", cell.content().rawContent());
        assertEquals("original", snapshot.get(a1).content().rawContent());
        assertEquals("replacement", sheet.getRawContent(a1));
    }

    @Test
    void formulaReferencesAndInvalidSyntaxAreStoredWithoutProcessing() {
        sheet.setContent(a1, "=A1");
        sheet.setContent(CellAddress.parse("B1"), "=MAX(A1;C2;100)");
        sheet.setContent(CellAddress.parse("C1"), "=A1:B5");
        sheet.setContent(CellAddress.parse("D1"), "=1+");
        assertEquals("=A1", sheet.getRawContent(a1));
        assertEquals("=MAX(A1;C2;100)", sheet.getRawContent(CellAddress.parse("B1")));
        assertEquals("=A1:B5", sheet.getRawContent(CellAddress.parse("C1")));
        assertEquals("=1+", sheet.getRawContent(CellAddress.parse("D1")));
    }

    @Test
    void cellsRequireBothAnAddressAndContent() {
        assertThrows(NullPointerException.class, () -> new Cell(null, EmptyContent.INSTANCE));
        assertThrows(NullPointerException.class, () -> new Cell(a1, null));
    }
}
