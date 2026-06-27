package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * Service implementation
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
@Service
@Slf4j
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        //Cache penetration
//        Shop shop = queryWithPassThrough(id);
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //Mutex against cache breakdown
//        Shop shop = queryWithMutex(id);
        //Logical expiration against cache breakdown
//        Shop shop = queryWithLogicalExpire(id);
        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if (shop == null) {
            shop = getById(id);
            if (shop != null) {
                cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + id, shop, CACHE_SHOP_TTL, TimeUnit.MINUTES);
            }
        }
        if (shop == null) {
            return Result.fail("Shop not found");
        }
        return Result.ok(shop);
    }

    /**
     * Mutex against cache breakdown
     *
     * @param id id
     * @return {@link Shop}
     */
    /*private Shop queryWithMutex(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //Query from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //Check existence
        if (StringUtils.isNotEmpty(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        if ("".equals(shopJson)) {
            return null;
        }
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            if (!isLock) {
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            shop = getById(id);
            Thread.sleep(200);
            if (shop == null) {
                //Write null placeholder
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //Write DB result to Redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            unLock(lockKey);
        }
        //Return
        return shop;
    }*/

    /**
     * Logical expiration against cache breakdown
     *
     * @param id id
     * @return {@link Shop}
     */
    /*private Shop queryWithLogicalExpire(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //Query from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //Check existence
        if (StringUtils.isEmpty(shopJson)) {
            return null;
        }
        //Cache hit, deserialize
        RedisDate redisDate = JSONUtil.toBean(shopJson, RedisDate.class);
        JSONObject jsonObject = (JSONObject) redisDate.getData();
        Shop shop = BeanUtil.toBean(jsonObject, Shop.class);
        LocalDateTime expireTime = redisDate.getExpireTime();
        //Check logical expiration
        if (expireTime.isAfter(LocalDateTime.now())) {
            return shop;
        }
        //Expired
        String lockKey = LOCK_SHOP_KEY + id;
        boolean flag = tryLock(lockKey);
        if (flag) {
            //Rebuild cache asynchronously
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShopToRedis(id, 20L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    //Release lock
                    unLock(lockKey);
                }
            });
        }
        return shop;
    }*/

    /**
     */
   /* private Shop queryWithPassThrough(Long id) {
        String shopKey = CACHE_SHOP_KEY + id;
        //Query from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(shopKey);
        //Check existence
        if (StringUtils.isNotEmpty(shopJson)) {
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //Check null placeholder
        if ("".equals(shopJson)) {
            return null;
        }
        //Cache miss, query DB
        Shop shop = getById(id);
        if (shop == null) {
            //Write null placeholder
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        //Write DB result to Redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //Return
        return shop;
    }*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("ID is required");
        }
        //Update database
        updateById(shop);
        //Delete cache
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        if (x == null || y == null) {
            return queryShopByTypeFromDb(typeId, current);
        }
        try {
            //Compute pagination
            int from = (current - 1) * SystemConstants.MAX_PAGE_SIZE;
            int end = current * SystemConstants.MAX_PAGE_SIZE;
            //Query Redis GEO with pagination (Redis 6.2+ GEOSEARCH)
            String key = SHOP_GEO_KEY + typeId;
            GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                    .search(key
                            , GeoReference.fromCoordinate(x, y)
                            , new Distance(5000)
                            , RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                    );
            if (results == null) {
                return queryShopByTypeFromDb(typeId, current);
            }
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();
            if (content.size() < from) {
                return Result.ok(Collections.emptyList());
            }
            List<Long> ids = new ArrayList<>(content.size());
            Map<String, Distance> distanceMap = new HashMap<>();
            content.stream().skip(from).forEach(result -> {
                String shopId = result.getContent().getName();
                ids.add(Long.valueOf(shopId));
                distanceMap.put(shopId, result.getDistance());
            });
            String join = StrUtil.join(",", ids);
            List<Shop> shopList = lambdaQuery().in(Shop::getId, ids).last("order by field(id," + join + ")").list();
            for (Shop shop : shopList) {
                shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
            }
            return Result.ok(shopList);
        } catch (Exception e) {
            log.warn("Redis GEO query failed, falling back to DB: {}", e.getMessage());
            return queryShopByTypeFromDb(typeId, current);
        }
    }

    private Result queryShopByTypeFromDb(Integer typeId, Integer current) {
        Page<Shop> page = lambdaQuery()
                .eq(Shop::getTypeId, typeId)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        return Result.ok(page.getRecords());
    }

//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /*    *//**
     * Acquire lock
     *
     * @param key key
     * @return boolean
     *//*
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    *//**
     * Release lock
     *
     * @param key key
     *//*
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    *//**
     *//*
    public void saveShopToRedis(Long id, Long expireSeconds) throws InterruptedException {
        //Load shop from DB
        Shop shop = getById(id);
        Thread.sleep(200);
        //Wrap with logical expiration
        RedisDate redisDate = new RedisDate();
        redisDate.setData(shop);
        redisDate.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        //Write to Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisDate));
    }*/
}
