package com.hmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * RedisPayload
 *
 * @author hmdp
 * @date 2022/10/08
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
