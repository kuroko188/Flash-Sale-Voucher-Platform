package com.hmdp.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    @Test
    void encodeAndMatch() {
        String raw = "secret123";
        String encoded = PasswordEncoder.encode(raw);
        assertNotNull(encoded);
        assertTrue(encoded.contains("@"));
        assertTrue(PasswordEncoder.matches(encoded, raw));
    }

    @Test
    void matchesReturnsFalseForNullOrWrongPassword() {
        String encoded = PasswordEncoder.encode("secret123");
        assertFalse(PasswordEncoder.matches(null, "secret123"));
        assertFalse(PasswordEncoder.matches(encoded, null));
        assertFalse(PasswordEncoder.matches(encoded, "wrong"));
    }

    @Test
    void matchesThrowsForInvalidFormat() {
        assertThrows(RuntimeException.class, () -> PasswordEncoder.matches("no-at-sign", "secret"));
    }
}
