package org.example.spreadsheet.domain;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Immutable, one-based coordinate. Columns use uppercase spreadsheet notation. */
public record CellAddress(int column, int row) {
    private static final Pattern ADDRESS = Pattern.compile("([A-Z]+)([1-9][0-9]*)");

    public CellAddress {
        if (column < 1 || row < 1) {
            throw new IllegalArgumentException("Column and row must be positive: " + column + ", " + row);
        }
    }

    /**
     * Parses an address such as A1 or AA10 without trimming or normalization.
     *
     * @throws NullPointerException if address is null
     * @throws IllegalArgumentException if notation is invalid or exceeds integer capacity
     */
    public static CellAddress parse(String address) {
        Objects.requireNonNull(address, "address");
        Matcher matcher = ADDRESS.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Expected uppercase column letters and a positive row: " + address);
        }

        int column = 0;
        for (char letter : matcher.group(1).toCharArray()) {
            int digit = letter - 'A' + 1;
            if (column > (Integer.MAX_VALUE - digit) / 26) {
                throw new IllegalArgumentException("Column exceeds supported integer capacity: " + address);
            }
            column = column * 26 + digit;
        }
        try {
            return new CellAddress(column, Integer.parseInt(matcher.group(2)));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Row exceeds supported integer capacity: " + address, exception);
        }
    }

    @Override
    public String toString() {
        StringBuilder label = new StringBuilder();
        int remaining = column;
        while (remaining > 0) {
            remaining--;
            label.append((char) ('A' + remaining % 26));
            remaining /= 26;
        }
        return label.reverse().append(row).toString();
    }
}
