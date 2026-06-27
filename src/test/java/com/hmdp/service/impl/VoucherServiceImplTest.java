package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.service.ISeckillVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.hmdp.utils.RedisConstants.SECKILL_STOCK_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock
    private VoucherMapper voucherMapper;
    @Mock
    private ISeckillVoucherService seckillVoucherService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    @InjectMocks
    private VoucherServiceImpl voucherService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(voucherService, "baseMapper", voucherMapper);
    }

    @Test
    void queryVoucherOfShopReturnsList() {
        Voucher voucher = new Voucher();
        voucher.setId(1L);
        when(voucherMapper.queryVoucherOfShop(1L)).thenReturn(Collections.singletonList(voucher));

        Result result = voucherService.queryVoucherOfShop(1L);

        assertTrue(result.getSuccess());
        assertEquals(Collections.singletonList(voucher), result.getData());
    }

    @Test
    void addSeckillVoucherPersistsAndCachesStock() {
        Voucher voucher = new Voucher();
        voucher.setId(10L);
        voucher.setStock(100);
        voucher.setBeginTime(LocalDateTime.now());
        voucher.setEndTime(LocalDateTime.now().plusDays(1));

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doReturn(true).when(voucherService).save(any(Voucher.class));

        voucherService.addSeckillVoucher(voucher);

        verify(seckillVoucherService).save(any(SeckillVoucher.class));
        verify(valueOperations).set(SECKILL_STOCK_KEY + 10L, "100");
    }
}
