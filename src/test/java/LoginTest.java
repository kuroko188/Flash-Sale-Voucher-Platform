import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmdp.HmDianPingApplication;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@SpringBootTest(classes = HmDianPingApplication.class)
@Tag("integration")
public class LoginTest {
    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * Manual utility: generate load-test users. Not run in CI.
     */
    @Test
    @Disabled("Manual data seeding utility — run locally only")
    public void testGenerateUser(){
        Long phone=17600000000L;
        for (int i = 0; i < 1000; i++) {
            User user = new User();
            user.setPhone(phone.toString());
            phone++;
            //Generate random nickname
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            userMapper.insert(user);
        }
    }

    /**
     * Manual utility: export JMeter tokens. Not run in CI.
     */
    @Test
    @Disabled("Manual JMeter token export — run locally only")
    public void testLogin() throws IOException {
        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
        userLambdaQueryWrapper.gt(true,User::getId,1011L);
        List<User> users = userMapper.selectList(userLambdaQueryWrapper);
        File tokens=new File("D:\\Downloads\\apache-jmeter-5.5\\test\\tokens.txt");
        FileOutputStream fileOutputStream = new FileOutputStream(tokens);
        for (User user : users) {
            String token = UUID.randomUUID().toString(true);
            fileOutputStream.write(token.getBytes());
            fileOutputStream.write("\r\n".getBytes());
            //Convert UserDTO to map
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>()
                    , CopyOptions.create().setIgnoreNullValue(true)
                            .setFieldValueEditor(
                                    (name, value) -> value.toString()
                            ));
            //Save user to Redis
            stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, map);
            //Set TTL
            stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);
        }
    }
}
