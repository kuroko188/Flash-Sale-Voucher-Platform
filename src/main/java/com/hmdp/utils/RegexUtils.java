package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;

/**
 * @author hmdp
 */
public class RegexUtils {
    /**
   * @param phone
   * @return true:，false：
     */
    public static boolean isPhoneInvalid(String phone){
        return mismatch(phone, RegexPatterns.PHONE_REGEX);
    }
    /**
   * @param email
   * @return true:，false：
     */
    public static boolean isEmailInvalid(String email){
        return mismatch(email, RegexPatterns.EMAIL_REGEX);
    }

    /**
   * @param code
   * @return true:，false：
     */
    public static boolean isCodeInvalid(String code){
        return mismatch(code, RegexPatterns.VERIFY_CODE_REGEX);
    }

    // Return true when pattern does not match
    private static boolean mismatch(String str, String regex){
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }
}
