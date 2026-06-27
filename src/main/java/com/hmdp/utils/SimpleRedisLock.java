package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author hmdp
 * @date 2022/10/09
 */
public class SimpleRedisLock implements ILock {
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KET_PREFIX="lock:";
    private static final String ID_PREFIX= UUID.randomUUID().toString(true)+"-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT=new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(Long timeoutSec) {
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        //Acquire lock
        Boolean isSuccess = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(KET_PREFIX + name
                        , threadId
                        , timeoutSec
                        , TimeUnit.SECONDS);
        //Avoid NPE from autounboxing
        return Boolean.TRUE.equals(isSuccess);
    }

    @Override
    public void unLock() {
        /*//Get thread id
        String threadId =ID_PREFIX+ Thread.currentThread().getId();
        String id = stringRedisTemplate.opsForValue().get(KET_PREFIX + name);
        //Compare owner ids
        if (StringUtils.equals(id,threadId)){
            stringRedisTemplate.delete(KET_PREFIX + name);
        }*/
        //Use Lua script for atomic unlock
        stringRedisTemplate.execute(UNLOCK_SCRIPT
                , Collections.singletonList(KET_PREFIX+name)
                ,ID_PREFIX+Thread.currentThread().getId());
    }
}
