package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author hmdp
 * @date 2022/10/09
 */
@Component
public class RedisIdWorker {
    /**
     */
    private static final Long BEGIN_TIMESTAMP = 1640995200L;
    /**
     */
    private static final Integer COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Get id
     *
   * @param keyPrefix
     * @return {@link Long}
     */
    public Long nextId(String keyPrefix) {
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        //Generate sequence
        //Daily sequence key
        String today = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        //Increment counter
        Long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + today);
        return timestamp << COUNT_BITS|count ;
    }

}
