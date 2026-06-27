package com.hmdp.controller;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.SystemConstants;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * REST controller
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Resource
    public IShopService shopService;

    /**
     * Get shop by id
     * @param id Shop id
   * @return Payload
     */
    @GetMapping("/{id}")
    public Result queryShopById(@PathVariable("id") Long id) {
        return shopService.queryById(id);
    }

    /**
     * Create shop
   * @param shop Payload
     * @return Shop id
     */
    @PostMapping
    public Result saveShop(@RequestBody Shop shop) {
        // Persist to database
        shopService.save(shop);
        return Result.ok();
    }

    /**
     * Update shop
   * @param shop Payload
     */
    @PutMapping
    public Result updateShop(@RequestBody Shop shop) {
        // Persist to database
        return shopService.update(shop);
    }

    /**
     * List shops by type
   * @param typeId
   * @param current
     */
    @GetMapping("/of/type")
    public Result queryShopByType(
            @RequestParam("typeId") Integer typeId,
            @RequestParam(value = "current", defaultValue = "1") Integer current,
            @RequestParam(value = "x",required = false) Double x,
            @RequestParam(value = "y",required = false) Double y
    ) {
        return shopService.queryShopByType(typeId,current,x,y);
    }

    /**
   * @param name Shop namekey
   * @param current
     */
    @GetMapping("/of/name")
    public Result queryShopByName(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "current", defaultValue = "1") Integer current
    ) {
        // Paginate by type
        Page<Shop> page = shopService.query()
                .like(StrUtil.isNotBlank(name), "name", name)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // Return data
        return Result.ok(page.getRecords());
    }
}
