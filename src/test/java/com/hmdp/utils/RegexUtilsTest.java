package com.hmdp.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegexUtilsTest {

    @Test
    void isPhoneInvalid_acceptsValidPhone() {
        assertFalse(RegexUtils.isPhoneInvalid("13800138000"));
    }

    @Test
    void isPhoneInvalid_rejectsBlankOrInvalid() {
        assertTrue(RegexUtils.isPhoneInvalid(null));
        assertTrue(RegexUtils.isPhoneInvalid(""));
        assertTrue(RegexUtils.isPhoneInvalid("12345"));
        assertTrue(RegexUtils.isPhoneInvalid("23800138000"));
    }

    @Test
    void isEmailInvalid_acceptsValidEmail() {
        assertFalse(RegexUtils.isEmailInvalid("user@example.com"));
    }

    @Test
    void isEmailInvalid_rejectsBlankOrInvalid() {
        assertTrue(RegexUtils.isEmailInvalid(null));
        assertTrue(RegexUtils.isEmailInvalid("not-an-email"));
    }

    @Test
    void isCodeInvalid_acceptsSixDigits() {
        assertFalse(RegexUtils.isCodeInvalid("123456"));
    }

    @Test
    void isCodeInvalid_rejectsNonSixDigitCode() {
        assertTrue(RegexUtils.isCodeInvalid(null));
        assertTrue(RegexUtils.isCodeInvalid("12345"));
        assertTrue(RegexUtils.isCodeInvalid("1234567"));
        assertTrue(RegexUtils.isCodeInvalid("12@456"));
    }
}
