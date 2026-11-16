package org.example.spreadsheet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Only tests starter wiring; does not validate spreadsheet functionality. */
class AppSmokeTest {
    @Test
    void scaffoldHasEntryPointMessage() {
        assertTrue(App.welcomeMessage().contains("scaffold ready"));
    }
}
