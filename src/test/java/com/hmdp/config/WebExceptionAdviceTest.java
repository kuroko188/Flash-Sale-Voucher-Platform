package com.hmdp.config;

import com.hmdp.dto.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebExceptionAdviceTest {

    private final WebExceptionAdvice advice = new WebExceptionAdvice();

    @Test
    void handleRuntimeExceptionReturnsFailResult() {
        Result result = advice.handleRuntimeException(new RuntimeException("boom"));
        assertFalse(result.getSuccess());
        assertEquals("Server error", result.getErrorMsg());
    }
}
