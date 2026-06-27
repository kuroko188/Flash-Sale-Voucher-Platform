package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * </p>
 *
 * @author hmdp
 * @date 2022/10/09
 * @since 2021-12-22
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    /**
     *
   * @param voucherId id
     * @return {@link Result}
     */
    Result seckillVoucher(Long voucherId);

    /**
     *
   * @param voucherId id
     * @return {@link Result}
     */
    Result getResult(Long voucherId);

    /**
     *
   * @param voucherOrder
     */
    @NotNull
    @Transactional(rollbackFor = Exception.class)
    void createVoucherOrder(VoucherOrder voucherOrder);
}
