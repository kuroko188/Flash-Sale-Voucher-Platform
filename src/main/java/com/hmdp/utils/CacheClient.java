package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * Redis cache helper
 *
 * @author hmdp
 * @date 2022/10/08
 */
@Slf4j
@Component
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;

    @Autowired
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Serialize object to JSON in Redis
     *
     * @param key   key
     * @param value value
     * @param time  TTL
     * @param unit  unit
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    /**
     *
     * @param key   key
     * @param value value
     * @param time  TTL
     * @param unit  unit
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //Wrap with logical expirationTTL
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    /**
     * Cache null values to prevent penetration
     *
   * @param keyPrefix key
     * @param id         id
   * @param type
     * @param dbFallback DB fallback
     * @param time       TTL
     * @param unit       unit
     * @return {@link R}
     */
    public <R, ID> R queryWithPassThrough(
            String keyPrefix
            , ID id
            , Class<R> type
            , Function<ID, R> dbFallback
            , Long time
            , TimeUnit unit) {
        String key = keyPrefix + id;
        //Query from Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        //Check existence
        if (StringUtils.isNotEmpty(json)) {
            return JSONUtil.toBean(json, type);
        }
        //Check null placeholder
        if ("".equals(json)) {
            return null;
        }
        //Cache miss, query DB
        R r = dbFallback.apply(id);
        if (r == null) {
            //Write null placeholder
            this.set(key, "", CACHE_NULL_TTL, TimeUnit.SECONDS);
            return null;
        }
        //Write DB result to Redis
        this.set(key, r, time, unit);
        //Return
        return r;
    }

    /**
     * Logical expiration against cache breakdown
     *
     * @param id id
     * @return {@link Shop}
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix
            , ID id
            , Class<R> type
            , Function<ID, R> dbFallback
            , Long time
            , TimeUnit unit) {
        String key = keyPrefix + id;
        //Query from Redis
        String json = stringRedisTemplate.opsForValue().get(key);
        //Check existence
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        //Cache hit, deserialize
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        JSONObject jsonObject = (JSONObject) redisData.getData();
        R r = BeanUtil.toBean(jsonObject, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //Check logical expiration
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
        //Expired
        String lockKey = LOCK_SHOP_KEY + id;
        boolean flag = tryLock(lockKey);
        if (flag) {
            //Rebuild cache asynchronously
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //Query database
                    R newR = dbFallback.apply(id);
                    //Write to Redis
                    this.setWithLogicalExpire(key,newR,time,unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //Release lock
                    unLock(lockKey);
                }
            });
        }
        return r;
    }

    /**
     * Cache rebuild thread pool
     */
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * Acquire lock
     *
     * @param key key
     * @return boolean
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * Release lock
     *
     * @param key key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
