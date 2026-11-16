package org.example.spreadsheet.storage;

import java.io.IOException;

/** Invalid or ambiguous S2V content that cannot be represented without data loss. */
public final class S2vFormatException extends IOException {
    public S2vFormatException(String message) {
        super(message);
    }

    public S2vFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
