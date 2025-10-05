package com.shikou.aicode.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.shikou.aicode.constant.UserConstant;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.entity.Vip;
import com.shikou.aicode.mapper.VipMapper;
import com.shikou.aicode.service.UserService;
import com.shikou.aicode.service.VipService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * VIP 服务层实现。
 *
 * @author Shikou
 */
@Service
public class VipServiceImpl extends ServiceImpl<VipMapper, Vip> implements VipService {

    @Lazy
    @Resource
    private UserService userService;

    @Override
    @Transactional
    public boolean redeem(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        boolean reduced = userService.reduceCoin(userId, UserConstant.VIP_REDEMPTION_COST);
        ThrowUtils.throwIf(!reduced, ErrorCode.OPERATION_ERROR, "你的硬币不足以兑换VIP");
        Vip vip = new Vip();
        vip.setUserId(userId);
        LocalDateTime expiredTime = LocalDateTime.now().plusDays(UserConstant.VIP_REDEMPTION_DAYS);
        // 如果用户本身是vip，那么在原本的时间上增加
        Vip userVip = getVip(userId);
        if (userVip != null && userVip.getExpiredTime().isAfter(expiredTime)) {
            expiredTime = userVip.getExpiredTime().plusDays(UserConstant.VIP_REDEMPTION_DAYS);
        }
        vip.setExpiredTime(expiredTime);
        return save(vip);
    }

    @Override
    public Vip getVip(Long userId) {
        QueryWrapper queryWrapper = QueryWrapper.create().eq(Vip::getUserId, userId);
        return getOne(queryWrapper);
    }

    @Override
    public boolean isVip(Long userId) {
        Vip vip = getVip(userId);
        if (vip == null) {
            return false;
        }
        LocalDateTime expiredTime = vip.getExpiredTime();
        if (expiredTime == null) {
            return false;
        }
        return !LocalDateTime.now().isAfter(expiredTime);
    }
}
