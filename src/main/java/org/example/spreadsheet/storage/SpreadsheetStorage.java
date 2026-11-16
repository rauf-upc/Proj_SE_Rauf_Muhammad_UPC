package org.example.spreadsheet.storage;

import org.example.spreadsheet.domain.Spreadsheet;

import java.io.IOException;
import java.nio.file.Path;

/** Persistence boundary, independent of the console and formula evaluation. */
public interface SpreadsheetStorage {
    /** Loads a new spreadsheet; never mutates an existing spreadsheet. */
    Spreadsheet load(Path source) throws IOException;

    /** Saves content, replacing an existing regular file only after writing succeeds. */
    void save(Spreadsheet spreadsheet, Path destination) throws IOException;
}
