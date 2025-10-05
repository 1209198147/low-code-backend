package com.shikou.aicode.ratelimit.annotation;

import com.shikou.aicode.ratelimit.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String key() default "";
    int rate() default 10;
    int rateInterval() default 1;
    RateLimitType type() default RateLimitType.USER;
    String message() default "请求过于频繁，请稍后再试";
}
