package com.hmdp.dto;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void okWithoutData() {
        Result result = Result.ok();
        assertTrue(result.getSuccess());
        assertNull(result.getErrorMsg());
        assertNull(result.getData());
    }

    @Test
    void okWithData() {
        Result result = Result.ok("token");
        assertTrue(result.getSuccess());
        assertEquals("token", result.getData());
    }

    @Test
    void okWithListAndTotal() {
        List<String> items = Arrays.asList("a", "b");
        Result result = Result.ok(items, 2L);
        assertTrue(result.getSuccess());
        assertEquals(items, result.getData());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void failReturnsErrorMessage() {
        Result result = Result.fail("Invalid phone number");
        assertFalse(result.getSuccess());
        assertEquals("Invalid phone number", result.getErrorMsg());
        assertNull(result.getData());
    }
}
