package org.example.spreadsheet.storage;

import org.example.spreadsheet.domain.CellAddress;
import org.example.spreadsheet.domain.FormulaContent;
import org.example.spreadsheet.domain.NumericContent;
import org.example.spreadsheet.domain.Spreadsheet;
import org.example.spreadsheet.domain.TextContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class S2vSpreadsheetStorageTest {
    @TempDir
    Path directory;

    private final SpreadsheetStorage storage = new S2vSpreadsheetStorage();

    @Test
    void loadsAndSavesTheAssignmentExampleWithoutCalculating() throws Exception {
        Path source = directory.resolve("example.s2v");
        try (InputStream input = getClass().getResourceAsStream("/example.s2v")) {
            assertNotNull(input);
            Files.copy(input, source);
        }
        Spreadsheet sheet = storage.load(source);
        assertEquals("=C1+C2", raw(sheet, "A1"));
        assertEquals("4", raw(sheet, "B1"));
        assertEquals("1", raw(sheet, "C1"));
        assertEquals("2", raw(sheet, "C2"));
        assertEquals("TOTAL", raw(sheet, "B3"));
        assertEquals("=A1+B1", raw(sheet, "C3"));
        for (String address : new String[]{"A2", "B2", "A3"}) {
            assertEquals("", raw(sheet, address));
        }
        assertInstanceOf(FormulaContent.class, sheet.getContent(CellAddress.parse("A1")));
        Path destination = directory.resolve("copy.s2v");
        storage.save(sheet, destination);
        assertEquals("=C1+C2;4;1\n;;2\n;TOTAL;=A1+B1\n", Files.readString(destination));
        assertEquals(sheet.getCells(), storage.load(destination).getCells());
    }

    @Test
    void emptySpreadsheetSavesAsZeroBytesAndLoadsAsEmpty() throws Exception {
        Path file = directory.resolve("empty.s2v");
        storage.save(new Spreadsheet(), file);
        assertEquals(0, Files.size(file));
        assertTrue(storage.load(file).getCells().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"42", "001.200", "-0.00", "TOTAL", "  UPC català  ", " ", "text,with,commas",
            "\"literal text quotes\"", "= 1 + 2 ", "=A1", "=A1:B5", "=1+", "="})
    void singleCellRoundTripsPreserveExactContentAndType(String value) throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        sheet.setContent(CellAddress.parse("A1"), value);
        Path file = directory.resolve("single.s2v");
        storage.save(sheet, file);
        Spreadsheet restored = storage.load(file);
        assertEquals(value, raw(restored, "A1"));
        assertEquals(sheet.getCells(), restored.getCells());
    }

    @Test
    void loadedNumbersTextAndFormulasRemainDistinct() throws Exception {
        Path file = write("types.s2v", "001.20;  header  ;=1+2;\n");
        Spreadsheet sheet = storage.load(file);
        assertInstanceOf(NumericContent.class, sheet.getContent(CellAddress.parse("A1")));
        assertInstanceOf(TextContent.class, sheet.getContent(CellAddress.parse("B1")));
        assertInstanceOf(FormulaContent.class, sheet.getContent(CellAddress.parse("C1")));
        assertEquals("001.20", raw(sheet, "A1"));
        assertEquals("  header  ", raw(sheet, "B1"));
        assertEquals("", raw(sheet, "D1"));
    }

    @Test
    void preservesLeadingBlankRowsCellsInternalGapsAndWideColumns() throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        sheet.setContent(CellAddress.parse("AA4"), "wide");
        sheet.setContent(CellAddress.parse("C2"), "middle");
        sheet.setContent(CellAddress.parse("A2"), "first");
        Path file = directory.resolve("gaps.s2v");
        storage.save(sheet, file);
        assertEquals("\nfirst;;middle\n\n" + ";".repeat(26) + "wide\n", Files.readString(file));
        assertEquals(sheet.getCells(), storage.load(file).getCells());
    }

    @Test
    void acceptsTrailingEmptyFieldsAndCanonicalizesThemAtTheLastPopulatedCell() throws Exception {
        Path input = write("trailing.s2v", "A;;\n;;B;;\n;;\n\n");
        Spreadsheet sheet = storage.load(input);
        assertEquals("A", raw(sheet, "A1"));
        assertEquals("B", raw(sheet, "C2"));
        assertEquals("", raw(sheet, "E2"));
        assertEquals("", raw(sheet, "C3"));
        Path output = directory.resolve("canonical.s2v");
        storage.save(sheet, output);
        assertEquals("A\n;;B\n", Files.readString(output));
        assertEquals(sheet.getCells(), storage.load(output).getCells());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", ";;\n;\n\n", "\r\n\r\n"})
    void allBlankRowsAndFieldsAreSemanticallyEmpty(String contents) throws Exception {
        Path file = write("blank.s2v", contents);
        Spreadsheet sheet = storage.load(file);
        assertTrue(sheet.getCells().isEmpty());
        storage.save(sheet, file);
        assertEquals("", Files.readString(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r"})
    void acceptsLineEndingsRaggedRowsAndMissingFinalNewline(String ending) throws Exception {
        Spreadsheet sheet = storage.load(write("lines.s2v", "first" + ending + ";second;third"));
        assertEquals("first", raw(sheet, "A1"));
        assertEquals("", raw(sheet, "B1"));
        assertEquals("", raw(sheet, "A2"));
        assertEquals("second", raw(sheet, "B2"));
        assertEquals("third", raw(sheet, "C2"));
    }

    @Test
    void serializesFunctionArgumentsIncludingTheNestedAssignmentFormula() throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        String nested = "=1 + A1*((SUMA(A2:B5;PROMEDIO(B6:D8);C1;27)/4)+(D6-D8))";
        sheet.setContent(CellAddress.parse("A1"), "=MAX(A1;C2;100)");
        sheet.setContent(CellAddress.parse("B1"), "text,with,commas");
        sheet.setContent(CellAddress.parse("A2"), nested);
        Path file = directory.resolve("functions.s2v");
        storage.save(sheet, file);
        assertEquals("=MAX(A1,C2,100);text,with,commas\n" + nested.replace(';', ',') + "\n",
                Files.readString(file));
        assertEquals(sheet.getCells(), storage.load(file).getCells());
        assertEquals(nested, raw(sheet, "A2")); // Saving did not change the original sheet.
    }

    @Test
    void overwritingReplacesAllPriorContentsIncludingWhenSavingAnEmptySheet() throws Exception {
        Path file = write("existing.s2v", "old;extra;cells\nlast row\n");
        Spreadsheet sheet = new Spreadsheet();
        sheet.setContent(CellAddress.parse("A1"), "new");
        storage.save(sheet, file);
        assertEquals("new\n", Files.readString(file));
        storage.save(new Spreadsheet(), file);
        assertEquals(0, Files.size(file));
        assertNoTemporaryFiles();
    }

    @ParameterizedTest
    @ValueSource(strings = {"text;literal", "text\nline", "text\rline", "text\0nul", "=1;2",
            "=MAX(1,2)", "=MAX(\"a;b\";1)"})
    void invalidExportsReportCellAndPreserveExistingDestination(String value) throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        sheet.setContent(CellAddress.parse("B2"), value);
        Path file = write("protected.s2v", "previous file\n");
        S2vFormatException error = assertThrows(S2vFormatException.class, () -> storage.save(sheet, file));
        assertTrue(error.getMessage().contains("B2"));
        assertEquals("previous file\n", Files.readString(file));
        assertEquals(value, raw(sheet, "B2"));
        assertNoTemporaryFiles();
    }

    @Test
    void invalidExportsDoNotCreateNewDestinationFiles() {
        Spreadsheet sheet = new Spreadsheet();
        sheet.setContent(CellAddress.parse("A1"), "invalid;literal");
        Path file = directory.resolve("not created.s2v");
        assertThrows(S2vFormatException.class, () -> storage.save(sheet, file));
        assertFalse(Files.exists(file));
    }

    @Test
    void encodingFailurePreservesExistingDestinationAndCleansTemporaryFile() throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        sheet.setContent(CellAddress.parse("A1"), "unpaired surrogate \uD800");
        Path file = write("encoding.s2v", "previous\n");
        assertThrows(IOException.class, () -> storage.save(sheet, file));
        assertEquals("previous\n", Files.readString(file));
        assertNoTemporaryFiles();
    }

    @Test
    void missingFilesAndDirectoryInputsProduceIoErrors() {
        assertThrows(IOException.class, () -> storage.load(directory.resolve("missing.s2v")));
        assertThrows(IOException.class, () -> storage.load(directory));
    }

    @Test
    void missingParentsAndDirectoryDestinationsProduceIoErrors() throws Exception {
        Spreadsheet sheet = new Spreadsheet();
        assertThrows(IOException.class, () -> storage.save(sheet, directory.resolve("missing").resolve("file.s2v")));
        Path existingDirectory = Files.createDirectory(directory.resolve("existing directory"));
        Path retained = Files.writeString(existingDirectory.resolve("retained.txt"), "keep");
        assertThrows(IOException.class, () -> storage.save(sheet, existingDirectory));
        assertEquals("keep", Files.readString(retained));
        assertNoTemporaryFiles();
    }

    @Test
    void malformedUtf8IsNotSilentlyReplaced() throws Exception {
        Path file = directory.resolve("bad encoding.s2v");
        Files.write(file, new byte[]{(byte) 0xC3, 0x28});
        assertThrows(IOException.class, () -> storage.load(file));
    }

    @Test
    void invalidStoredCellHasAnAddressInTheError() throws Exception {
        Path file = write("invalid.s2v", "first\n;bad\0text\n");
        S2vFormatException error = assertThrows(S2vFormatException.class, () -> storage.load(file));
        assertTrue(error.getMessage().contains("B2"));
    }

    @Test
    void quotedFormulaArgumentsAreRejectedOnLoadWithoutGuessing() throws Exception {
        Path file = write("quoted.s2v", "=MAX(\"a,b\",1)\n");
        assertThrows(S2vFormatException.class, () -> storage.load(file));
    }

    @Test
    void nullApiArgumentsAreExplicitlyRejected() {
        assertThrows(NullPointerException.class, () -> storage.load(null));
        assertThrows(NullPointerException.class, () -> storage.save(null, directory.resolve("file.s2v")));
        assertThrows(NullPointerException.class, () -> storage.save(new Spreadsheet(), null));
    }

    @Test
    void deterministicSparseGridRoundTripsAcrossSeveralColumnWidths() throws Exception {
        Random random = new Random(20261116);
        String[] contents = {"", "0", "001.20", " header ", "català", "= 1 + 2", "=MAX(A1;MIN(B2;7))"};
        Spreadsheet sheet = new Spreadsheet();
        for (int i = 0; i < 100; i++) {
            sheet.setContent(new CellAddress(1 + random.nextInt(55), 1 + random.nextInt(12)),
                    contents[random.nextInt(contents.length)]);
        }
        Path file = directory.resolve("grid.s2v");
        storage.save(sheet, file);
        assertEquals(sheet.getCells(), storage.load(file).getCells());
    }

    private Path write(String name, String contents) throws IOException {
        return Files.writeString(directory.resolve(name), contents, StandardCharsets.UTF_8);
    }

    private static String raw(Spreadsheet sheet, String address) {
        return sheet.getRawContent(CellAddress.parse(address));
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(directory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}
