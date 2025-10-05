package com.shikou.aicode.ratelimit.annotation;


import org.redisson.api.RateIntervalUnit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UserRateLimit {
    String key() default "";
    int rate() default 10;
    int vipRate() default 20;
    int rateInterval() default 1;
    RateIntervalUnit rateIntervalUnit() default RateIntervalUnit.SECONDS;
    String message() default "今日的请求次数已耗尽，请明天再试";
}
