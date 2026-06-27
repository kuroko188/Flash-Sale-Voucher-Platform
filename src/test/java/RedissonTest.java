import com.hmdp.HmDianPingApplication;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest(classes = HmDianPingApplication.class)
public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    private RLock rLock;
    @BeforeEach
    private void getRlock(){
        rLock=redissonClient.getLock("lock");
    }

    @Test
    public void method1() throws InterruptedException {
        boolean isLock = rLock.tryLock(1L, TimeUnit.SECONDS);
        if (!isLock){
            log.info("Acquire lock失败--1");
        }
        try {
            log.info("Acquire lock成功--1");
            method2();
            log.info("执行业务--1");
        }
        finally {
            log.info("准备Release lock--1");
            rLock.unlock();
        }
    }

    public void method2(){
        boolean isLock = rLock.tryLock();
        if (!isLock){
            log.info("Acquire lock失败--2");
        }
        try {
            log.info("Acquire lock成功--2");
            log.info("执行业务--2");
        }
        finally {
            log.info("准备Release lock--2");
            rLock.unlock();
        }
    }
}
