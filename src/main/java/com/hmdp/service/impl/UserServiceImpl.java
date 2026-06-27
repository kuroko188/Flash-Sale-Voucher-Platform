package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.*;


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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //Validate phone number
        if (RegexUtils.isPhoneInvalid(phone)) {
            //Invalid phone number
            return Result.fail("Invalid phone number");
        }
        //Valid phone, generate verification code
        String code = RandomUtil.randomNumbers(6);
        /*//Save code to session
        session.setAttribute("code", code);*/
        //Save code to Redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //Send verification code
        log.debug("Verification code sent: {}", code);
        return Result.ok(code);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //Validate phone number
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            //Invalid phone number
            return Result.fail("Invalid phone number");
        }
        //Load code from Redis and validate
        /*  Object cacheCode = session.getAttribute("code");*/
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.equals(code)) {
            //Mismatch, return error
            return Result.fail("Invalid verification code");
        }
        //Match, load user by phone
        User user = baseMapper
                .selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone));
        //Check whether user exists
        if (user == null) {
            //Create user if missing
            user = createUserWithPhone(phone);
        }
        /*//Save user to session
        session.setAttribute("user", BeanUtil.copyProperties(user, UserDTO.class));*/
        //Generate token
        String token = UUID.randomUUID().toString(true);
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
        return Result.ok(token);
    }

    @Override
    public Result sign() {
        //Get current user
        Long id = UserHolder.getUser().getId();
        //Get current date
        LocalDateTime now = LocalDateTime.now();
        //Build Redis key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY +yyyyMM+ id;
        //Get day of month
        int dayOfMonth = now.getDayOfMonth();
        //Write to Redis
        stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        //Get current user
        Long id = UserHolder.getUser().getId();
        //Get current date
        LocalDateTime now = LocalDateTime.now();
        //Build Redis key
        String yyyyMM = now.format(DateTimeFormatter.ofPattern("yyyy:MM:"));
        String key = USER_SIGN_KEY +yyyyMM+ id;
        //Get day of month
        int dayOfMonth = now.getDayOfMonth();
        //Load sign-in bits for this month
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key
                , BitFieldSubCommands
                        .create()
                        .get(BitFieldSubCommands.BitFieldType
                                .unsigned(dayOfMonth))
                        .valueAt(0)
        );
        if (result==null||result.isEmpty()){
            return Result.ok(0);
        }
        Long num = result.get(0);
        if (num==null||num==0){
            return Result.ok(0);
        }
        //Convert to binary string
        String binaryString = Long.toBinaryString(num);
        //Count consecutive sign-in days
        int count=0;
        for (int i = binaryString.length()-1; i >=0; i--) {
            if (binaryString.charAt(i)=='1'){
                count++;
            }
            else {
                break;
            }
        }
        //Return
        return Result.ok(count);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        //Generate random nickname
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        baseMapper.insert(user);
        return user;
    }
}
