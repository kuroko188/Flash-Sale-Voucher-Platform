package com.hmdp.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleRedisLockTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void tryLockReturnsTrueWhenAcquired() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        SimpleRedisLock lock = new SimpleRedisLock("order:1", stringRedisTemplate);
        assertTrue(lock.tryLock(10L));
    }

    @Test
    void tryLockReturnsFalseWhenNotAcquired() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(10L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);

        SimpleRedisLock lock = new SimpleRedisLock("order:1", stringRedisTemplate);
        assertFalse(lock.tryLock(10L));
    }

    @Test
    void unLockExecutesLuaScript() {
        SimpleRedisLock lock = new SimpleRedisLock("order:1", stringRedisTemplate);
        lock.unLock();
        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(Collections.singletonList("lock:order:1")),
                anyString()
        );
    }
}
