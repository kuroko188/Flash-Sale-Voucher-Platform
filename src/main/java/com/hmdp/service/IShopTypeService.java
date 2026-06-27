package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;

/**
 * <p>
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {

    /**
     *
     * @return {@link Result}
     */
    Result getTypeList();
}
