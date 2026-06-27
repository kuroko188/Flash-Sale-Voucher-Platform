package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 * </p>
 *
 * @author hmdp
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

    /**
     * Send verification code
     *
     * @param phone   Phone number
   * @param session
     * @return {@link Result}
     */
    Result sendCode(String phone, HttpSession session);

    /**
     *
   * @param loginForm
   * @param session
     * @return {@link Result}
     */
    Result login(LoginFormDTO loginForm, HttpSession session);

    /**
     *
     * @return {@link Result}
     */
    Result sign();

    /**
     *
     * @return {@link Result}
     */
    Result signCount();
}
