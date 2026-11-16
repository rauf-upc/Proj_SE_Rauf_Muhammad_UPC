package org.example.spreadsheet.cli;

import org.example.spreadsheet.domain.CellAddress;
import org.example.spreadsheet.storage.S2vSpreadsheetStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SpreadsheetCliTest {
    @TempDir
    Path directory;

    @Test
    void demonstratesCreateReplaceViewSaveLoadAndRestoredRawContents() throws Exception {
        Path file = directory.resolve("round trip.s2v");
        String output = run("new\nset\nA1\nold\nset\nA1\n=MAX(B1;MIN(C1;100))\n"
                + "set\nB1\n001.20\nset\nC1\n  UPC  \nshow\nA1\nsave\n" + file
                + "\nnew\nshow\nA1\nload\n" + file + "\nshow\nA1\nshow\nB1\nshow\nC1\nexit\n");
        assertEquals("=MAX(B1,MIN(C1,100));001.20;  UPC  \n", Files.readString(file));
        assertTrue(output.contains("A1 content: []"));
        assertEquals(2, output.split("A1 content: \\[=MAX", -1).length - 1);
        assertTrue(output.contains("B1 content: [001.20]"));
        assertTrue(output.contains("C1 content: [  UPC  ]"));
        assertTrue(output.contains("Saved: " + file));
        assertTrue(output.contains("Loaded: " + file));
        assertFalse(output.contains("Error:"));
    }

    @Test
    void clearingContentAndCreatingANewSheetWork() throws Exception {
        String output = run("set\nA1\nvalue\nset\nA1\n\nshow\nA1\nset\nB1\nother\nnew\nshow\nB1\nexit\n");
        assertTrue(output.contains("A1 content: []"));
        assertTrue(output.contains("B1 content: []"));
        assertFalse(output.contains("Error:"));
    }

    @Test
    void failedLoadLeavesTheCurrentSpreadsheetIntact() throws Exception {
        Path missing = directory.resolve("missing.s2v");
        String output = run("set\nA1\nretained\nload\n" + missing + "\nshow\nA1\nexit\n");
        assertTrue(output.contains("Error:"));
        assertFalse(output.contains("Loaded:"));
        assertTrue(output.contains("A1 content: [retained]"));
    }

    @Test
    void failedLoadAfterAValidRowDoesNotInstallPartialData() throws Exception {
        Path file = Files.writeString(directory.resolve("invalid.s2v"), "replacement\n=MAX(\"a,b\",1)\n");
        String output = run("set\nA1\nretained\nload\n" + file + "\nshow\nA1\nshow\nA2\nexit\n");
        assertTrue(output.contains("Error: Cell A2:"));
        assertTrue(output.contains("A1 content: [retained]"));
        assertTrue(output.contains("A2 content: []"));
        assertFalse(output.contains("Loaded:"));
    }

    @Test
    void invalidAddressesDoNotChangePreviousContentAndUnknownCommandsAreExplained() throws Exception {
        String output = run("set\nA1\nprevious\nset\nA0\nunknown\nshow\nA1\nexit\n");
        assertTrue(output.contains("Error: Expected uppercase"));
        assertTrue(output.contains("Error: choose new, set, show, save, load, or exit."));
        assertTrue(output.contains("A1 content: [previous]"));
    }

    @Test
    void failedSaveDoesNotReportSuccessOrDamageTheFileOrCell() throws Exception {
        Path file = Files.writeString(directory.resolve("retained.s2v"), "previous file\n");
        String output = run("set\nA1\ntext;literal\nsave\n" + file + "\nshow\nA1\nexit\n");
        assertTrue(output.contains("Error: Cell A1:"));
        assertFalse(output.contains("Saved:"));
        assertTrue(output.contains("A1 content: [text;literal]"));
        assertEquals("previous file\n", Files.readString(file));
    }

    @Test
    void directoryDestinationsAndInvalidPathCharactersProduceErrors() throws Exception {
        String output = run("save\n" + directory + "\nload\nbad\0path\nshow\nA1\nexit\n");
        assertTrue(output.contains("Error: Save destination must be a regular file"));
        assertTrue(output.contains("A1 content: []"));
        assertFalse(output.contains("Saved:"));
        assertFalse(output.contains("Loaded:"));
    }

    @Test
    void endOfInputDuringAnOperationIsReportedAndReturns() throws Exception {
        StringWriter output = new StringWriter();
        new SpreadsheetCli(new StringReader("set\nA1\n"), output, new S2vSpreadsheetStorage()).run();
        assertTrue(output.toString().contains("Input ended before the operation was complete"));
    }

    @Test
    void immediateEndOfInputExitsWithoutAnError() throws Exception {
        assertFalse(run("").contains("Error:"));
    }

    @Test
    void selfReferencesAndIncompleteExpressionsSurviveTheCliRoundTrip() throws Exception {
        Path file = directory.resolve("opaque.s2v");
        String output = run("set\nA1\n=A1\nset\nB1\n=1+\nsave\n" + file + "\nnew\nload\n"
                + file + "\nshow\nA1\nshow\nB1\nexit\n");
        assertTrue(output.contains("A1 content: [=A1]"));
        assertTrue(output.contains("B1 content: [=1+]"));
        assertEquals("=A1", new S2vSpreadsheetStorage().load(file).getRawContent(CellAddress.parse("A1")));
    }

    private static String run(String commands) throws Exception {
        StringWriter output = new StringWriter();
        new SpreadsheetCli(new StringReader(commands), output, new S2vSpreadsheetStorage()).run();
        return output.toString();
    }
}
