package com.hmdp.config;

import com.hmdp.interceptor.LoginInterceptor;
import com.hmdp.interceptor.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * MVC configuration
 *
 * @author hmdp
 * @date 2022/10/07
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/**")
                .addResourceLocations(
                        "classpath:/nginx-1.18.0/html/hmdp/",
                        "classpath:/static/"
                );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //Login interceptor
        registry
                .addInterceptor(new LoginInterceptor())
                .excludePathPatterns("/"
                        , "/index.html"
                        , "/login.html"
                        , "/seckill.html"
                        , "/user/code"
                        , "/user/login"
                        , "/voucher/list/**"
                        , "/css/**"
                        , "/js/**"
                        , "/imgs/**"
                        , "/favicon.ico"
                )
                .order(1);
        //Token refresh interceptor
        registry
                .addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
    }
}
