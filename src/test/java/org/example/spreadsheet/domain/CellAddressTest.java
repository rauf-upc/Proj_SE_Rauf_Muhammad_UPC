package org.example.spreadsheet.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class CellAddressTest {
    @ParameterizedTest
    @CsvSource({"A1,1,1", "B5,2,5", "Z10,26,10", "AA1,27,1", "AZ9,52,9",
            "BA10,53,10", "ZZ1,702,1", "AAA10,703,10",
            "FXSHRXW2147483647,2147483647,2147483647"})
    void parsesAndFormatsCoordinates(String notation, int column, int row) {
        CellAddress address = CellAddress.parse(notation);
        assertEquals(column, address.column());
        assertEquals(row, address.row());
        assertEquals(notation, address.toString());
        assertEquals(new CellAddress(column, row), address);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A0", "A01", "A-1", "1A", "A", "1", "a1", "Aa1",
            " A1", "A1 ", "A1\n", "A 1", "A1:B2", "$A$1", "Ä1", "A１",
            "A2147483648", "FXSHRXX1", "ZZZZZZZZZZZZZZZZ1"})
    void rejectsInvalidNotationAndOverflow(String notation) {
        assertThrows(IllegalArgumentException.class, () -> CellAddress.parse(notation));
    }

    @ParameterizedTest
    @CsvSource({"0,1", "1,0", "-1,1", "1,-1", "-2147483648,1"})
    void directConstructionAlsoValidatesBounds(int column, int row) {
        assertThrows(IllegalArgumentException.class, () -> new CellAddress(column, row));
    }

    @ParameterizedTest
    @NullSource
    void rejectsNull(String notation) {
        assertThrows(NullPointerException.class, () -> CellAddress.parse(notation));
    }

    @Test
    void equalCoordinatesActAsTheSameMapKey() {
        var cells = new HashMap<CellAddress, String>();
        cells.put(new CellAddress(27, 10), "original");
        assertEquals("original", cells.get(CellAddress.parse("AA10")));
        assertNotEquals(CellAddress.parse("AA10"), CellAddress.parse("AA11"));
        assertNotEquals(CellAddress.parse("AA10"), CellAddress.parse("AB10"));
    }
}
