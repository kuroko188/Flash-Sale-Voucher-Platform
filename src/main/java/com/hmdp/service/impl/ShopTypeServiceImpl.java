package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  Service implementation
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result getTypeList() {
        String typeKey= RedisConstants.CACHE_TYPE_KEY;
        //Query from Redis
        Long typeListSize = stringRedisTemplate.opsForList().size(typeKey);
        //Data exists in Redis
        if (typeListSize!=null&&typeListSize!=0){
            List<String> typeJsonList = stringRedisTemplate.opsForList().range(typeKey, 0, typeListSize-1);
            List<ShopType> typeList=new ArrayList<>();
            for (String typeJson : typeJsonList) {
                typeList.add(JSONUtil.toBean(typeJson,ShopType.class));
            }
            return Result.ok(typeList);
        }
        //Cache miss, query database
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (typeList==null){
            //No data in database
            return Result.fail("Something went wrong");
        }
        //Convert
        List<String> typeJsonList=new ArrayList<>();
        for (ShopType shopType : typeList) {
            typeJsonList.add(JSONUtil.toJsonStr(shopType));
        }
        //Write database result to Redis
        stringRedisTemplate.opsForList().rightPushAll(typeKey,typeJsonList);
        //Return data
        return Result.ok(typeList);
    }
}
