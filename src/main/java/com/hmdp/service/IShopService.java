package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;

/**
 * <p>
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {

    /**
     *
     * @param id id
     * @return {@link Result}
     */
    Result queryById(Long id);

    /**
     *
   * @param shop
     * @return {@link Result}
     */
    Result update(Shop shop);

    /**
     *
   * @param typeId id
   * @param current
     * @param x       x
     * @param y       y
     * @return {@link Result}
     */
    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);
}
