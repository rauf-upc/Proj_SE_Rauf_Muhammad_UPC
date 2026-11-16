package org.example.spreadsheet.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class S2vFormulaCodecTest {
    @ParameterizedTest
    @ValueSource(strings = {"=MAX(A1;C2;100)", "=SUMA(1;MIN(2;3);PROMEDIO(A1:B3;7))",
            "=1 + A1*((SUMA(A2:B5;PROMEDIO(B6:D8);C1;27)/4)+(D6-D8))",
            "=MAX ((1+2);3)", "=Unknown_Name(1;2)", "=MAX(1;2"})
    void convertsOnlyArgumentDelimitersAndReversesThem(String raw) throws Exception {
        String encoded = S2vFormulaCodec.encode(raw);
        assertFalse(encoded.contains(";"));
        assertEquals(raw.replace(';', ','), encoded);
        assertEquals(raw, S2vFormulaCodec.decode(encoded));
    }

    @ParameterizedTest
    @ValueSource(strings = {"= 1 + 2 ", "=A1", "=1+", "=", "=A1:B5", "=1,5", "=(1,2)",
            "ordinary,text", "=\"text without delimiters\""})
    void leavesOtherRawCharactersAndMalformedExpressionsAlone(String raw) throws Exception {
        assertEquals(raw, S2vFormulaCodec.encode(raw));
        assertEquals(raw, S2vFormulaCodec.decode(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {"=1;2", "=MAX(1,2)", "=MAX((1;2);3)", "=MAX(\"a;b\";1)",
            "=MAX('a,b';1)", "=MAX(1;2);3"})
    void refusesAmbiguousOrUnrepresentableFormulaSerialization(String raw) {
        assertThrows(S2vFormatException.class, () -> S2vFormulaCodec.encode(raw));
    }

    @Test
    void refusesQuotedFileArgumentsWithoutInventingEscaping() {
        assertThrows(S2vFormatException.class, () -> S2vFormulaCodec.decode("=MAX(\"a,b\",1)"));
    }
}
