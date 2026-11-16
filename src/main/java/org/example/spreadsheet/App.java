package org.example.spreadsheet;

import org.example.spreadsheet.cli.SpreadsheetCli;
import org.example.spreadsheet.storage.S2vSpreadsheetStorage;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/** Application entry point; spreadsheet behavior belongs to domain and storage. */
public final class App {
    private App() {
    }

    public static String welcomeMessage() {
        return "Spreadsheet project scaffold ready. Implement milestones in order.";
    }

    public static void main(String[] args) throws IOException {
        new SpreadsheetCli(new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8),
                new S2vSpreadsheetStorage()).run();
    }
}
