package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

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
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * Self-inject for proxy; @Lazy avoids circular dependency
     */
    @Resource
    @Lazy
    private IVoucherOrderService voucherOrderService;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    //    private static final BlockingQueue<VoucherOrder> orderTasks=new ArrayBlockingQueue<>(1024*1024);
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "voucher-order-stream-consumer");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean running = true;

    @PostConstruct
    private void init() {
        running = true;
        ensureStreamConsumerGroup();
        SECKILL_ORDER_EXECUTOR.submit(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    //Read order from stream
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from(ORDER_STREAM_GROUP, ORDER_STREAM_CONSUMER)
                            , StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2))
                            , StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.lastConsumed())
                    );
                    //Check whether message was read
                    if (list==null||list.isEmpty()){
                        //No message, continue loop
                        continue;
                    }
                    //Parse message
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> values = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                    //Create order
                    handleVoucherOrder(voucherOrder);
                    //ACK message
                    stringRedisTemplate.opsForStream().acknowledge(ORDER_STREAM_KEY, ORDER_STREAM_GROUP, record.getId());
                } catch (IllegalStateException e) {
                    if (!running) {
                        break;
                    }
                    log.warn("Redis stream consumer connection is unavailable, retrying: {}", e.getMessage());
                    handlePendingList();
                } catch (Exception e) {
                    if (!running) {
                        break;
                    }
                    if (isNoGroupError(e)) {
                        ensureStreamConsumerGroup();
                        continue;
                    }
                    log.warn("Redis stream consumer error, retrying", e);
                    handlePendingList();
                }
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void ensureStreamConsumerGroup() {
        try {
            stringRedisTemplate.execute((RedisCallback<Void>) connection -> {
                connection.streamCommands().xGroupCreate(
                        ORDER_STREAM_KEY.getBytes(StandardCharsets.UTF_8),
                        ORDER_STREAM_GROUP,
                        ReadOffset.latest(),
                        true
                );
                return null;
            });
            log.info("Created Redis stream consumer group {} on {}", ORDER_STREAM_GROUP, ORDER_STREAM_KEY);
        } catch (Exception e) {
            String message = e.getMessage() == null ? "" : e.getMessage();
            if (message.contains("BUSYGROUP")) {
                log.debug("Redis stream consumer group already exists: {}", ORDER_STREAM_GROUP);
                return;
            }
            log.warn("Failed to ensure Redis stream consumer group: {}", message);
        }
    }

    private boolean isNoGroupError(Exception e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("NOGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @PreDestroy
    private void destroy() {
        running = false;
        SECKILL_ORDER_EXECUTOR.shutdownNow();
    }

    private void handlePendingList() {
        while (true){
            try {
                //Read order from stream
                List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                        Consumer.from(ORDER_STREAM_GROUP, ORDER_STREAM_CONSUMER)
                        , StreamReadOptions.empty().count(1)
                        , StreamOffset.create(ORDER_STREAM_KEY, ReadOffset.from("0"))
                );
                //Check whether message was read
                if (list==null||list.isEmpty()){
                    //No message, continue loop
                    break;
                }
                //Parse message
                MapRecord<String, Object, Object> record = list.get(0);
                Map<Object, Object> values = record.getValue();
                VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(values, new VoucherOrder(), true);
                //Create order
                handleVoucherOrder(voucherOrder);
                //ACK message
                stringRedisTemplate.opsForStream().acknowledge(ORDER_STREAM_KEY, ORDER_STREAM_GROUP, record.getId());
            } catch (Exception e) {
                if (isNoGroupError(e)) {
                    ensureStreamConsumerGroup();
                    break;
                }
                log.warn("Failed to process pending stream orders", e);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        //Create lock (fallback)
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //Acquire lock
        boolean isLock = lock.tryLock();
        if (!isLock) {
            throw new RuntimeException("Unknown error");
        }
        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
        } finally {
            //Release lock
            lock.unlock();
        }

    }

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    /**
     *
   * @param voucherId id
     * @return {@link Result}
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        //Get user
        UserDTO user = UserHolder.getUser();
        //Generate order id
        Long orderId = redisIdWorker.nextId("order");
        //Run Lua script
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                , user.getId().toString()
                , orderId.toString());
        //Check Lua result
        int r = res.intValue();
        if (r != 0) {
            //Non-zero result means not eligible
            return Result.fail(r == 1 ? "Out of stock" : "Duplicate order not allowed");
        }
        return Result.ok(orderId);
    }
    /**
     *
   * @param voucherId id
     * @return {@link Result}
     */
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //Get user
        UserDTO user = UserHolder.getUser();
        //Run Lua script
        Long res = stringRedisTemplate.execute(
                SECKILL_SCRIPT
                , Collections.emptyList()
                , voucherId.toString()
                ,user.getId().toString());
        //Check Lua result
        int r=res.intValue();
        if (r!=0){
            //Non-zero result means not eligible
            return Result.fail(r==1?"Out of stock":"Duplicate order not allowed");
        }
        Long orderId = redisIdWorker.nextId("order");
        //Create order
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setId(orderId);
        orderTasks.add(voucherOrder);
        return Result.ok(orderId);
    }*/

    /**
     *
   * @param voucherId id
     * @return {@link Result}
     */
    /*@Override
    public Result seckillVoucher(Long voucherId) {
        //Load seckill voucher
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //Check seckill start time
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //Seckill has not started
            return Result.fail("Seckill has not started");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            //Seckill has ended
            return Result.fail("Seckill has ended");
        }
        //Check stock
        if (voucher.getStock() < 1) {
            //Out of stock
            return Result.fail("Out of stock");
        }
        Long userId = UserHolder.getUser().getId();
        //Single-instance only
//        synchronized (userId.toString().intern()) {
//            //Use self-injection for proxy
//            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
//            return voucherOrderService.getResult(voucherId);
//        }
        //Create lock
//        SimpleRedisLock simpleRedisLock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //Acquire lock
//        boolean isLock = simpleRedisLock.tryLock(1200L);
        boolean isLock = lock.tryLock();
        if (!isLock){
            return Result.fail("One order per user");
        }
        try {
            return voucherOrderService.getResult(voucherId);
        } finally {
            //Release lock
            lock.unlock();
        }
    }*/
    @Override
    @NotNull
    @Transactional(rollbackFor = Exception.class)
    public Result getResult(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        Long count = lambdaQuery()
                .eq(VoucherOrder::getVoucherId, voucherId)
                .eq(VoucherOrder::getUserId, userId)
                .count();
        if (count > 0) {
            return Result.fail("Duplicate purchase not allowed");
        }
        //Decrease stock
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherId)
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock=stock-1"));
        if (!isSuccess) {
            //Out of stock
            return Result.fail("Out of stock");
        }
        //Create order
        VoucherOrder voucherOrder = new VoucherOrder();
        Long orderId = redisIdWorker.nextId("order");
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setId(orderId);
        this.save(voucherOrder);
        return Result.ok(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //Decrease stock
        boolean isSuccess = seckillVoucherService.update(
                new LambdaUpdateWrapper<SeckillVoucher>()
                        .eq(SeckillVoucher::getVoucherId, voucherOrder.getVoucherId())
                        .gt(SeckillVoucher::getStock, 0)
                        .setSql("stock=stock-1"));
        //Create order
        this.save(voucherOrder);
    }
}
