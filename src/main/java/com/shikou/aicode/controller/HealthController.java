package com.shikou.aicode.controller;

import com.shikou.aicode.common.BaseResponse;
import com.shikou.aicode.common.ResultUtils;
import com.shikou.aicode.ratelimit.annotation.RateLimit;
import com.shikou.aicode.ratelimit.enums.RateLimitType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

    @RateLimit(rate = 5, rateInterval = 10, type = RateLimitType.IP)
    @GetMapping("/")
    public BaseResponse<String> healthCheck() {
        return ResultUtils.success("ok");
    }
}
