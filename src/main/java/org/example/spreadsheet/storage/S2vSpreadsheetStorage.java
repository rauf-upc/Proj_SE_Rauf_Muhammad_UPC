package org.example.spreadsheet.storage;

import org.example.spreadsheet.domain.Cell;
import org.example.spreadsheet.domain.CellAddress;
import org.example.spreadsheet.domain.Spreadsheet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.TreeMap;

/** UTF-8 S2V raw-content storage following the assignment's delimiter rules. */
public final class S2vSpreadsheetStorage implements SpreadsheetStorage {
    @Override
    public Spreadsheet load(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        Spreadsheet loaded = new Spreadsheet();
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            long row = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                if (row > Integer.MAX_VALUE) {
                    throw new S2vFormatException("Row exceeds supported integer capacity");
                }
                String[] fields = line.split(";", -1);
                for (int index = 0; index < fields.length; index++) {
                    CellAddress address = new CellAddress(index + 1, (int) row);
                    try {
                        validateField(fields[index]);
                        loaded.setContent(address, S2vFormulaCodec.decode(fields[index]));
                    } catch (S2vFormatException exception) {
                        throw atCell(address, exception);
                    }
                }
                row++;
            }
        }
        return loaded;
    }

    @Override
    public void save(Spreadsheet spreadsheet, Path destination) throws IOException {
        Objects.requireNonNull(spreadsheet, "spreadsheet");
        Objects.requireNonNull(destination, "destination");
        // Validate and encode a stable snapshot before touching any destination file.
        TreeMap<Integer, TreeMap<Integer, String>> rows = new TreeMap<>();
        for (Cell cell : spreadsheet.getCells().values()) {
            String encoded;
            try {
                validateField(cell.content().rawContent());
                encoded = S2vFormulaCodec.encode(cell.content().rawContent());
                if (encoded.indexOf(';') >= 0) {
                    throw new S2vFormatException("Literal text semicolons have no specified S2V escaping rule");
                }
            } catch (S2vFormatException exception) {
                throw atCell(cell.address(), exception);
            }
            rows.computeIfAbsent(cell.address().row(), ignored -> new TreeMap<>())
                    .put(cell.address().column(), encoded);
        }

        Path target = destination.toAbsolutePath();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                && !Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Save destination must be a regular file: " + target);
        }
        Path temporary = Files.createTempFile(target.getParent(), "spreadsheet ", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writeRows(rows, writer);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    private static void writeRows(TreeMap<Integer, TreeMap<Integer, String>> rows, BufferedWriter writer)
            throws IOException {
        if (rows.isEmpty()) {
            return;
        }
        for (long row = 1; row <= rows.lastKey(); row++) {
            TreeMap<Integer, String> columns = rows.get((int) row);
            if (columns != null) {
                for (long column = 1; column <= columns.lastKey(); column++) {
                    if (column > 1) {
                        writer.write(';');
                    }
                    writer.write(columns.getOrDefault((int) column, ""));
                }
            }
            writer.write('\n');
        }
    }

    private static void validateField(String raw) throws S2vFormatException {
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0 || raw.indexOf('\0') >= 0) {
            throw new S2vFormatException("Cell contains a line break or NUL; S2V has no escaping rule for it");
        }
    }

    private static S2vFormatException atCell(CellAddress address, S2vFormatException cause) {
        return new S2vFormatException("Cell " + address + ": " + cause.getMessage(), cause);
    }
}
