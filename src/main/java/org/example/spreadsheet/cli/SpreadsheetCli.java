package org.example.spreadsheet.cli;

import org.example.spreadsheet.domain.Cell;
import org.example.spreadsheet.domain.CellAddress;
import org.example.spreadsheet.domain.Spreadsheet;
import org.example.spreadsheet.storage.SpreadsheetStorage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Question/answer console adapter; domain rules and S2V encoding stay separate. */
public final class SpreadsheetCli {
    private static final String COMMANDS = "new | set | show | table | save | saveas | load | help | exit";
    private final BufferedReader input;
    private final BufferedWriter output;
    private final SpreadsheetStorage storage;
    private Spreadsheet spreadsheet = new Spreadsheet();
    private Map<CellAddress, Cell> savedCells = Map.of();
    private Path currentFile;

    /** Streams belong to the caller and are not closed by this adapter. */
    public SpreadsheetCli(Reader input, Writer output, SpreadsheetStorage storage) {
        this.input = new BufferedReader(Objects.requireNonNull(input, "input"));
        this.output = new BufferedWriter(Objects.requireNonNull(output, "output"));
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public void run() throws IOException {
        print("New empty spreadsheet. View shows raw content; formulas are not calculated.\n");
        while (true) {
            print(COMMANDS + "\nChoice: ");
            String command = input.readLine();
            if (command == null) {
                if (hasUnsavedChanges()) {
                    print("Input ended with unsaved changes; no changes were saved.\n");
                }
                return;
            }
            try {
                switch (command.strip().toLowerCase(Locale.ROOT)) {
                    case "new" -> newSpreadsheet();
                    case "set" -> setContent();
                    case "show" -> showContent();
                    case "table" -> showTable();
                    case "save" -> save(false);
                    case "saveas" -> save(true);
                    case "load" -> load();
                    case "help" -> showHelp();
                    case "exit" -> {
                        if (confirmDiscard()) {
                            print("Goodbye.\n");
                            return;
                        }
                    }
                    default -> print("Error: unknown command. Type help for available operations.\n");
                }
            } catch (IOException | IllegalArgumentException exception) {
                print("Error: " + exception.getMessage() + "\n");
            }
        }
    }

    private void newSpreadsheet() throws IOException {
        if (confirmDiscard()) {
            spreadsheet = new Spreadsheet();
            savedCells = Map.of();
            currentFile = null;
            print("Created empty spreadsheet.\n");
        }
    }

    private void setContent() throws IOException {
        CellAddress address = CellAddress.parse(prompt("Cell address: "));
        spreadsheet.setContent(address, prompt("Raw content (empty clears): "));
        print("Set " + address + ".\n");
    }

    private void showContent() throws IOException {
        CellAddress address = CellAddress.parse(prompt("Cell address: "));
        print(address + " content: [" + spreadsheet.getRawContent(address) + "]\n");
    }

    private void showTable() throws IOException {
        Map<CellAddress, Cell> cells = spreadsheet.getCells();
        print("Populated cells only. Backslashes, tabs and line breaks are escaped for display.\n");
        if (cells.isEmpty()) {
            print("No populated cells.\n");
            return;
        }
        print("Cell | Raw content\n");
        for (Cell cell : cells.values().stream()
                .sorted(Comparator.comparingInt((Cell value) -> value.address().row())
                        .thenComparingInt(value -> value.address().column())).toList()) {
            String display = cell.content().rawContent().replace("\\", "\\\\")
                    .replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
            print(cell.address() + " | [" + display + "]\n");
        }
    }

    private void save(boolean saveAs) throws IOException {
        String instruction = !saveAs && currentFile != null
                ? "File path (empty uses " + currentFile + "): " : "File path: ";
        String entered = prompt(instruction);
        Path path;
        if (entered.isEmpty() && !saveAs && currentFile != null) {
            path = currentFile;
        } else {
            path = requirePath(entered);
        }
        storage.save(spreadsheet, path);
        // A failed save must not change the remembered file or the saved snapshot.
        currentFile = path;
        savedCells = spreadsheet.getCells();
        print("Saved: " + path + "\n");
    }

    private void load() throws IOException {
        Path path = requirePath(prompt("File path: "));
        // Validate the entire candidate first; failed loads never ask to discard edits.
        Spreadsheet candidate = storage.load(path);
        if (confirmDiscard()) {
            spreadsheet = candidate;
            currentFile = path;
            savedCells = spreadsheet.getCells();
            print("Loaded: " + path + "\n");
        }
    }

    private static Path requirePath(String entered) {
        if (entered.isBlank()) {
            throw new IllegalArgumentException("File path must not be empty");
        }
        return Path.of(entered);
    }

    private boolean hasUnsavedChanges() {
        return !spreadsheet.getCells().equals(savedCells);
    }

    private boolean confirmDiscard() throws IOException {
        while (hasUnsavedChanges()) {
            String answer = prompt("Unsaved changes. Discard them? [yes/no, empty=no]: ")
                    .strip().toLowerCase(Locale.ROOT);
            if (answer.equals("yes")) {
                return true;
            }
            if (answer.equals("no") || answer.isEmpty()) {
                print("Cancelled; current spreadsheet retained.\n");
                return false;
            }
            print("Enter yes or no.\n");
        }
        return true;
    }

    private void showHelp() throws IOException {
        print("set/show: enter an uppercase address such as A1 or AA10, then raw content for set.\n"
                + "An empty content line clears a cell; spaces and the '=' prefix are preserved.\n"
                + "table: list populated cell contents in row/column order.\n"
                + "save: enter a path, or an empty line to reuse the current file.\n"
                + "saveas: enter a new path; existing regular files are replaced on successful save.\n"
                + "load/new/exit: unsaved edits require confirmation before being discarded.\n"
                + "Paths may contain spaces; enter them without surrounding quotes.\n");
    }

    private String prompt(String message) throws IOException {
        print(message);
        String value = input.readLine();
        if (value == null) {
            throw new EOFException("Input ended before the operation was complete");
        }
        return value;
    }

    private void print(String message) throws IOException {
        output.write(message);
        output.flush();
    }
}
