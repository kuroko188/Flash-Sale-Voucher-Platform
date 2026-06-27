package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;

/**
 * <p>
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     *
   * @param followUserId User id
   * @param isFollow
     * @return {@link Result}
     */
    Result follow(Long followUserId, Boolean isFollow);

    /**
     *
   * @param followUserId User id
     * @return {@link Result}
     */
    Result isFollow(Long followUserId);

    /**
     *
     * @param id id
     * @return {@link Result}
     */
    Result followCommons(Long id);
}
