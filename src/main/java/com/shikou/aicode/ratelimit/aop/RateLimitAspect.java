package com.shikou.aicode.ratelimit.aop;

import cn.hutool.core.util.StrUtil;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.ratelimit.annotation.RateLimit;
import com.shikou.aicode.ratelimit.annotation.UserRateLimit;
import com.shikou.aicode.service.UserService;
import com.shikou.aicode.service.VipService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private UserService userService;
    @Resource
    private VipService vipService;

    @Order(1)
    @Before("@annotation(rateLimit)")
    public void doLimit(JoinPoint joinPoint, RateLimit rateLimit){
        String key = generateRateLimitKey(joinPoint, rateLimit);
        int rate = rateLimit.rate();
        int rateInterval = rateLimit.rateInterval();
        String message = rateLimit.message();

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.expire(Duration.ofHours(1));
        rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, RateIntervalUnit.SECONDS);
        if(!rateLimiter.tryAcquire(1)){
            if(StringUtils.isEmpty(message)){
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
            }
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, message);
        }
    }

    @Order(2)
    @Before("@annotation(rateLimit)")
    public void doUserLimit(JoinPoint joinPoint, UserRateLimit rateLimit){
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        int dayOfYear = LocalDate.now().getDayOfYear();
        String key = StrUtil.format("user_rate_limit:{}:{}:{}", rateLimit.key(), dayOfYear, userId);
        int rate = rateLimit.rate();
        int vipRate = rateLimit.vipRate();
        int rateInterval = rateLimit.rateInterval();
        String message = rateLimit.message();
        RateIntervalUnit rateIntervalUnit = rateLimit.rateIntervalUnit();

        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.expire(Duration.ofDays(1));
        if(vipService.isVip(userId)){
            rateLimiter.trySetRate(RateType.OVERALL, vipRate, rateInterval, rateIntervalUnit);
        }else{
            rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, rateIntervalUnit);
        }
        if(!rateLimiter.tryAcquire(1)){
            if(StringUtils.isEmpty(message)){
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
            }
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, message);
        }
    }

    public String generateRateLimitKey(JoinPoint joinPoint, RateLimit rateLimit){
        String key = rateLimit.key();
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_limit:");
        if(StringUtils.isNotEmpty(key)){
            keyBuilder.append(key).append(":");
        }
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        switch (rateLimit.type()){
            case IP -> keyBuilder.append("ip:").append(getClientIP(request));
            case API -> {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                String className = method.getDeclaringClass().getSimpleName();
                String methodName = method.getName();
                keyBuilder.append("api:").append(className).append(".").append(methodName);
            }
            case USER -> {
                try{
                    // 获取当前登录用户
                    User loginUser = userService.getLoginUser(request);
                    keyBuilder.append("user:").append(loginUser.getId());
                }catch (Exception e){
                    // 获取不到用户按IP限流
                    keyBuilder.append("ip:").append(getClientIP(request));
                }
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的限流类型");
        }
        return keyBuilder.toString();
    }
    private String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
