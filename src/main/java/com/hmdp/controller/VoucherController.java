package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;
import com.hmdp.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Voucher REST controller.
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return Result.ok(voucher.getId());
    }

    /**
     * Sample seckill voucher payload:
     * {
     *   "shopId": 1,
     *   "title": "100 off voucher",
     *   "subTitle": "Valid every day",
     *   "rules": "General use",
     *   "payValue": 8000,
     *   "actualValue": 10000,
     *   "type": 1,
     *   "stock": 100,
     *   "beginTime": "2026-06-12T00:00:00",
     *   "endTime": "2026-12-31T23:59:59"
     * }
     */
    @PostMapping("seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return Result.ok(voucher.getId());
    }

    @GetMapping("/list/{shopId}")
    public Result queryVoucherOfShop(@PathVariable("shopId") Long shopId) {
       return voucherService.queryVoucherOfShop(shopId);
    }
}
