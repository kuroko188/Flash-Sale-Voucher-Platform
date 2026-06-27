package com.hmdp.utils;

/**
 *
 * @author hmdp
 * @date 2022/10/09
 */
public interface ILock {
    /**
     *
   * @param timeoutSec Release lock
   * @return Acquire lock true false
     */
    boolean tryLock(Long timeoutSec);

    /**
     * Release lock
     */
    void unLock();
}
