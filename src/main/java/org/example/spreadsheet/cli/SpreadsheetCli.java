package org.example.spreadsheet.cli;

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
import java.util.Locale;
import java.util.Objects;

/** Small question/answer console adapter; owns no domain or S2V implementation. */
public final class SpreadsheetCli {
    private final BufferedReader input;
    private final BufferedWriter output;
    private final SpreadsheetStorage storage;
    private Spreadsheet spreadsheet = new Spreadsheet();

    /** Streams belong to the caller and are not closed by this adapter. */
    public SpreadsheetCli(Reader input, Writer output, SpreadsheetStorage storage) {
        this.input = new BufferedReader(Objects.requireNonNull(input, "input"));
        this.output = new BufferedWriter(Objects.requireNonNull(output, "output"));
        this.storage = Objects.requireNonNull(storage, "storage");
    }

    public void run() throws IOException {
        print("New empty spreadsheet. View shows raw content; formulas are not calculated.\n");
        while (true) {
            print("new | set | show | save | load | exit\nChoice: ");
            String command = input.readLine();
            if (command == null) {
                return;
            }
            try {
                switch (command.strip().toLowerCase(Locale.ROOT)) {
                    case "new" -> {
                        spreadsheet = new Spreadsheet();
                        print("Created empty spreadsheet.\n");
                    }
                    case "set" -> {
                        CellAddress address = CellAddress.parse(prompt("Cell address: "));
                        spreadsheet.setContent(address, prompt("Raw content (empty clears): "));
                        print("Set " + address + ".\n");
                    }
                    case "show" -> {
                        CellAddress address = CellAddress.parse(prompt("Cell address: "));
                        print(address + " content: [" + spreadsheet.getRawContent(address) + "]\n");
                    }
                    case "save" -> {
                        Path path = Path.of(prompt("File path: "));
                        storage.save(spreadsheet, path);
                        print("Saved: " + path + "\n");
                    }
                    case "load" -> {
                        Path path = Path.of(prompt("File path: "));
                        // Assignment happens only after a complete successful load.
                        spreadsheet = storage.load(path);
                        print("Loaded: " + path + "\n");
                    }
                    case "exit" -> {
                        print("Goodbye.\n");
                        return;
                    }
                    default -> print("Error: choose new, set, show, save, load, or exit.\n");
                }
            } catch (IOException | IllegalArgumentException exception) {
                print("Error: " + exception.getMessage() + "\n");
            }
        }
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
