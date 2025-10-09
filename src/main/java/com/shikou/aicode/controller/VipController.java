package com.shikou.aicode.controller;

import com.shikou.aicode.annotation.AuthCheck;
import com.shikou.aicode.common.BaseResponse;
import com.shikou.aicode.common.ResultUtils;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.service.VipService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VIP 控制层。
 *
 * @author Shikou
 */
@RestController
@RequestMapping("/vip")
public class VipController {

    @Resource
    private VipService vipService;


    @PostMapping("/redeem")
    @AuthCheck
    public BaseResponse<Boolean> Redeem(HttpServletRequest request) {
        ThrowUtils.throwIf(request==null, ErrorCode.PARAMS_ERROR);
        boolean result = vipService.redeem(request);
        return ResultUtils.success(result);
    }

}
