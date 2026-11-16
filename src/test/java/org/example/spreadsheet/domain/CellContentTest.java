package org.example.spreadsheet.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CellContentTest {
    @ParameterizedTest
    @ValueSource(strings = {"0", "42", "-12", "+7", "001.20", "-0.00", ".5", "-.5", "12.",
            "123456789012345678901234567890.123456789"})
    void numbersKeepTheirExactSpellingWithoutPrecisionLoss(String raw) {
        CellContent content = CellContent.fromRaw(raw);
        assertInstanceOf(NumericContent.class, content);
        assertEquals(raw, content.rawContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOTAL", "  Header  ", " ", "42 ", " 42", " =1+2", "1e3", "1,5",
            "NaN", "Infinity", "1+2", "+", ".", "--2", "UPC BarcelonaTech — català", "a;b,c\nnext"})
    void textRemainsUnchangedIncludingUnsupportedNumericSpellings(String raw) {
        CellContent content = CellContent.fromRaw(raw);
        assertInstanceOf(TextContent.class, content);
        assertEquals(raw, content.rawContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"=1+2", "= 1 + 2 ", "=A1", "=MAX(A1;C2;100)", "=A1:B5",
            "=SUMA(A1;MIN(B2;3))", "=1+", "=", "=unknown\nraw"})
    void formulasAreOpaqueSourceIncludingInvalidExpressions(String raw) {
        CellContent content = CellContent.fromRaw(raw);
        assertInstanceOf(FormulaContent.class, content);
        assertEquals(raw, content.rawContent());
    }

    @Test
    void emptyContentIsDistinctFromZeroAndSpaces() {
        assertSame(EmptyContent.INSTANCE, CellContent.fromRaw(""));
        assertEquals("", EmptyContent.INSTANCE.rawContent());
        assertInstanceOf(NumericContent.class, CellContent.fromRaw("0"));
        assertInstanceOf(TextContent.class, CellContent.fromRaw(" "));
    }

    @Test
    void nullIsRejectedByEveryRawContentEntryPoint() {
        assertThrows(NullPointerException.class, () -> CellContent.fromRaw(null));
        assertThrows(NullPointerException.class, () -> new NumericContent(null));
        assertThrows(NullPointerException.class, () -> new TextContent(null));
        assertThrows(NullPointerException.class, () -> new FormulaContent(null));
    }

    @Test
    void directConstructorsCannotCreateContradictoryCategories() {
        assertThrows(IllegalArgumentException.class, () -> new NumericContent("TOTAL"));
        assertThrows(IllegalArgumentException.class, () -> new NumericContent(""));
        assertThrows(IllegalArgumentException.class, () -> new NumericContent("1e3"));
        assertThrows(IllegalArgumentException.class, () -> new FormulaContent("1+2"));
        assertThrows(IllegalArgumentException.class, () -> new FormulaContent(""));
        assertThrows(IllegalArgumentException.class, () -> new TextContent(""));
        assertThrows(IllegalArgumentException.class, () -> new TextContent("42"));
        assertThrows(IllegalArgumentException.class, () -> new TextContent("=1+2"));
    }
}
