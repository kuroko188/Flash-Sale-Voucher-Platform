package com.hmdp.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenInterceptorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private RefreshTokenInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RefreshTokenInterceptor(stringRedisTemplate);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void preHandleContinuesWhenNoToken() throws Exception {
        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(UserHolder.getUser());
        verifyNoInteractions(stringRedisTemplate);
    }

    @Test
    void preHandleContinuesWhenTokenNotInRedis() throws Exception {
        request.addHeader("authorization", "abc123");
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(RedisConstants.LOGIN_USER_KEY + "abc123"))
                .thenReturn(Collections.emptyMap());

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertNull(UserHolder.getUser());
    }

    @Test
    void preHandleLoadsUserAndRefreshesTtl() throws Exception {
        request.addHeader("authorization", "abc123");
        Map<Object, Object> userMap = new HashMap<>();
        userMap.put("id", "1");
        userMap.put("nickName", "Alex");

        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(RedisConstants.LOGIN_USER_KEY + "abc123")).thenReturn(userMap);

        assertTrue(interceptor.preHandle(request, response, new Object()));

        UserDTO user = UserHolder.getUser();
        assertNotNull(user);
        assertEquals(1L, user.getId());
        assertEquals("Alex", user.getNickName());
        verify(stringRedisTemplate).expire(
                RedisConstants.LOGIN_USER_KEY + "abc123",
                RedisConstants.LOGIN_USER_TTL,
                TimeUnit.MINUTES
        );
    }

    @Test
    void afterCompletionRemovesUser() throws Exception {
        UserHolder.saveUser(new UserDTO());
        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(UserHolder.getUser());
    }
}
