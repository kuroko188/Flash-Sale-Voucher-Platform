package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private HttpSession session;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void sendCodeRejectsInvalidPhone() {
        Result result = userService.sendCode("123", session);
        assertFalse(result.getSuccess());
        assertEquals("Invalid phone number", result.getErrorMsg());
    }

    @Test
    void sendCodeStoresCodeInRedis() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Result result = userService.sendCode("13800138000", session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        verify(valueOperations).set(
                startsWith(RedisConstants.LOGIN_CODE_KEY),
                anyString(),
                eq(RedisConstants.LOGIN_CODE_TTL),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void loginRejectsInvalidPhone() {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("bad");
        form.setCode("123456");

        Result result = userService.login(form, session);
        assertFalse(result.getSuccess());
        assertEquals("Invalid phone number", result.getErrorMsg());
    }

    @Test
    void loginRejectsWrongCode() {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("13800138000");
        form.setCode("123456");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + form.getPhone())).thenReturn("000000");

        Result result = userService.login(form, session);
        assertFalse(result.getSuccess());
        assertEquals("Invalid verification code", result.getErrorMsg());
    }

    @Test
    void loginCreatesUserAndReturnsToken() {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("13800138000");
        form.setCode("123456");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + form.getPhone())).thenReturn("123456");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        });

        Result result = userService.login(form, session);

        assertTrue(result.getSuccess());
        assertNotNull(result.getData());
        verify(hashOperations).putAll(startsWith(RedisConstants.LOGIN_USER_KEY), anyMap());
        verify(stringRedisTemplate).expire(
                startsWith(RedisConstants.LOGIN_USER_KEY),
                eq(RedisConstants.LOGIN_USER_TTL),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void loginUsesExistingUser() {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("13800138000");
        form.setCode("123456");
        User existing = new User();
        existing.setId(7L);
        existing.setPhone(form.getPhone());
        existing.setNickName("Alex");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(valueOperations.get(RedisConstants.LOGIN_CODE_KEY + form.getPhone())).thenReturn("123456");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        Result result = userService.login(form, session);

        assertTrue(result.getSuccess());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void signWritesBitForCurrentUser() {
        com.hmdp.dto.UserDTO user = new com.hmdp.dto.UserDTO();
        user.setId(5L);
        UserHolder.saveUser(user);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Result result = userService.sign();
        assertTrue(result.getSuccess());
        verify(valueOperations).setBit(startsWith(RedisConstants.USER_SIGN_KEY), anyLong(), eq(true));
    }

    @Test
    void signCountReturnsZeroWhenNoBits() {
        com.hmdp.dto.UserDTO user = new com.hmdp.dto.UserDTO();
        user.setId(5L);
        UserHolder.saveUser(user);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.bitField(anyString(), any(BitFieldSubCommands.class))).thenReturn(null);

        Result result = userService.signCount();
        assertTrue(result.getSuccess());
        assertEquals(0, result.getData());
    }

    @Test
    void signCountReturnsConsecutiveDays() {
        com.hmdp.dto.UserDTO user = new com.hmdp.dto.UserDTO();
        user.setId(5L);
        UserHolder.saveUser(user);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.bitField(anyString(), any(BitFieldSubCommands.class)))
                .thenReturn(Collections.singletonList(7L)); // binary 111 -> 3 consecutive days

        Result result = userService.signCount();
        assertTrue(result.getSuccess());
        assertEquals(3, result.getData());
    }
}
