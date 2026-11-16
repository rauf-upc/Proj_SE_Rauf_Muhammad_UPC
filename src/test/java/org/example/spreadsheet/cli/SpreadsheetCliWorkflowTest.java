package org.example.spreadsheet.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.example.spreadsheet.storage.S2vSpreadsheetStorage;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SpreadsheetCliWorkflowTest {
    @TempDir
    Path directory;

    @Test
    void emptyDiscardAnswerKeepsTheCurrentSheet() throws Exception {
        String output = run("set\nA1\n= 1 + 2\nnew\n\nshow\nA1\nexit\nyes\n");
        assertTrue(output.contains("Cancelled; current spreadsheet retained."));
        assertTrue(output.contains("A1 content: [= 1 + 2]"));
        assertTrue(output.contains("Goodbye."));
        assertFalse(output.contains("Error:"));
    }

    @Test
    void affirmativeDiscardCreatesABlankSheet() throws Exception {
        String output = run("set\nA1\nold\nnew\nyes\nshow\nA1\nexit\n");
        assertTrue(output.contains("Created empty spreadsheet."));
        assertTrue(output.contains("A1 content: []"));
        assertEquals(1, occurrences(output, "Unsaved changes."));
    }

    @Test
    void exitCanBeCancelledAndEditingCanContinue() throws Exception {
        String output = run("set\nA1\noriginal\nexit\nno\nset\nA1\nchanged\nshow\nA1\nexit\nyes\n");
        assertTrue(output.contains("A1 content: [changed]"));
        assertEquals(1, occurrences(output, "Goodbye."));
        assertFalse(output.contains("Error:"));
    }

    @Test
    void invalidDiscardAnswersDoNotSilentlyAuthorizeDiscarding() throws Exception {
        String output = run("set\nA1\nkeep\nnew\nmaybe\nNO\nshow\nA1\nexit\nYES\n");
        assertTrue(output.contains("Enter yes or no."));
        assertTrue(output.contains("A1 content: [keep]"));
    }

    @Test
    void noOpEditsAndRestoringSavedContentsDoNotNeedConfirmation() throws Exception {
        Path file = Files.writeString(directory.resolve("saved.s2v"), "original\n");
        String output = run("load\n" + file + "\nset\nA1\noriginal\nset\nB1\n\n"
                + "set\nA1\ntemporary\nset\nA1\noriginal\nexit\n");
        assertFalse(output.contains("Unsaved changes."));
        assertTrue(output.contains("Goodbye."));
        assertEquals("original\n", Files.readString(file));
    }

    @Test
    void cancellingLoadPreservesEditsAndTheCurrentFile() throws Exception {
        Path current = Files.writeString(directory.resolve("current.s2v"), "saved\n");
        Path candidate = Files.writeString(directory.resolve("candidate.s2v"), "candidate\n");
        String output = run("load\n" + current + "\nset\nA1\nretained\nload\n" + candidate
                + "\nno\nshow\nA1\nsave\n\nexit\n");
        assertTrue(output.contains("A1 content: [retained]"));
        assertFalse(output.contains("Loaded: " + candidate));
        assertEquals("retained\n", Files.readString(current));
        assertEquals("candidate\n", Files.readString(candidate));
    }

    @Test
    void confirmedLoadAssociatesTheLoadedFileForTheNextSave() throws Exception {
        Path candidate = Files.writeString(directory.resolve("candidate.s2v"), "loaded\n");
        String output = run("set\nA1\ndiscarded\nload\n" + candidate
                + "\nyes\nshow\nA1\nset\nA1\nupdated\nsave\n\nexit\n");
        assertTrue(output.contains("A1 content: [loaded]"));
        assertEquals("updated\n", Files.readString(candidate));
        assertFalse(output.contains("Error:"));
    }

    @Test
    void failedLoadDoesNotAskToDiscardOrResetTheCurrentFile() throws Exception {
        Path current = Files.writeString(directory.resolve("current.s2v"), "original\n");
        String output = run("load\n" + current + "\nset\nA1\nedited\nload\n"
                + directory.resolve("missing.s2v") + "\nsave\n\nexit\n");
        assertTrue(output.contains("Error:"));
        assertFalse(output.contains("Unsaved changes."));
        assertEquals("edited\n", Files.readString(current));
    }

    @Test
    void saveAsChangesTheCurrentFileWithoutOverwritingTheOldOne() throws Exception {
        Path original = Files.writeString(directory.resolve("original.s2v"), "old\n");
        Path copy = directory.resolve("new copy.s2v");
        String output = run("load\n" + original + "\nset\nA1\n=MAX(B1;2)\nsaveas\n" + copy
                + "\nset\nB1\n001.20\nsave\n\nexit\n");
        assertEquals("old\n", Files.readString(original));
        assertEquals("=MAX(B1,2);001.20\n", Files.readString(copy));
        assertFalse(output.contains("Error:"));
        assertFalse(output.contains("Unsaved changes."));
    }

    @Test
    void failedSaveAsRetainsThePreviousFileAssociation() throws Exception {
        Path current = Files.writeString(directory.resolve("current.s2v"), "original\n");
        String output = run("load\n" + current + "\nset\nA1\nedited\nsaveas\n" + directory
                + "\nsave\n\nexit\n");
        assertTrue(output.contains("Error: Save destination must be a regular file"));
        assertEquals("edited\n", Files.readString(current));
        assertEquals(1, occurrences(output, "Saved:"));
    }

    @Test
    void failedSaveStillRequiresConfirmationBeforeDiscardingEdits() throws Exception {
        Path file = directory.resolve("invalid.s2v");
        String output = run("set\nA1\ntext;literal\nsave\n" + file
                + "\nnew\nno\nshow\nA1\nexit\nyes\n");
        assertTrue(output.contains("A1 content: [text;literal]"));
        assertEquals(2, occurrences(output, "Unsaved changes."));
        assertFalse(Files.exists(file));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void saveWithoutACurrentFileRequiresANonblankPath(String path) throws Exception {
        String output = run("save\n" + path + "\nexit\n");
        assertTrue(output.contains("Error: File path must not be empty"));
        assertFalse(output.contains("Saved:"));
    }

    @Test
    void saveAsRequiresANewPathEvenWhenACurrentFileExists() throws Exception {
        Path current = Files.writeString(directory.resolve("current.s2v"), "original\n");
        String output = run("load\n" + current + "\nsaveas\n\nexit\n");
        assertTrue(output.contains("Error: File path must not be empty"));
        assertEquals("original\n", Files.readString(current));
    }

    @Test
    void newClearsTheRememberedFileAsWellAsTheCells() throws Exception {
        Path file = Files.writeString(directory.resolve("current.s2v"), "keep\n");
        String output = run("load\n" + file + "\nnew\nsave\n\nexit\n");
        assertTrue(output.contains("Error: File path must not be empty"));
        assertEquals("keep\n", Files.readString(file));
    }

    @Test
    void endOfInputReportsUnsavedDataWithoutSavingAutomatically() throws Exception {
        String output = run("set\nA1\nnot saved\n");
        assertTrue(output.contains("Input ended with unsaved changes; no changes were saved."));
        assertFalse(output.contains("Saved:"));
    }

    @Test
    void tableShowsPopulatedCellsInRowAndNumericColumnOrderWithoutCalculation() throws Exception {
        String output = run("set\nA2\nlast row\nset\nAA1\n= 1 + 2\nset\nZ1\n001.20\n"
                + "set\nB1\n  heading  \ntable\nexit\nyes\n");
        assertTrue(output.contains("Cell | Raw content"));
        assertTrue(output.contains("AA1 | [= 1 + 2]"));
        assertTrue(output.contains("Z1 | [001.20]"));
        assertTrue(output.indexOf("B1 | [  heading  ]") < output.indexOf("Z1 | [001.20]"));
        assertTrue(output.indexOf("Z1 | [001.20]") < output.indexOf("AA1 | [= 1 + 2]"));
        assertTrue(output.indexOf("AA1 | [= 1 + 2]") < output.indexOf("A2 | [last row]"));
        assertFalse(output.lines().anyMatch(line -> line.startsWith("A1 | [")));
    }

    @Test
    void tableDisplayEscapesDoNotChangeStoredTabsOrBackslashes() throws Exception {
        Path file = directory.resolve("display.s2v");
        String raw = "path\\name\tend";
        String output = run("set\nA1\n" + raw + "\ntable\nsaveas\n" + file + "\nexit\n");
        assertTrue(output.contains("A1 | [path\\\\name\\tend]"));
        assertEquals(raw + "\n", Files.readString(file));
    }

    @Test
    void emptyTableAndHelpDoNotMarkTheSpreadsheetAsModified() throws Exception {
        String output = run("table\nhelp\nexit\n");
        assertTrue(output.contains("No populated cells."));
        assertTrue(output.contains("saveas: enter a new path"));
        assertFalse(output.contains("Unsaved changes."));
        assertFalse(output.contains("Error:"));
    }

    private static int occurrences(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    private static String run(String commands) throws Exception {
        StringWriter output = new StringWriter();
        new SpreadsheetCli(new StringReader(commands), output, new S2vSpreadsheetStorage()).run();
        return output.toString();
    }
}
