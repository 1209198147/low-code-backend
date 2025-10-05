package com.shikou.aicode.service;

import com.mybatisflex.core.service.IService;
import com.shikou.aicode.model.entity.Vip;
import jakarta.servlet.http.HttpServletRequest;

/**
 * VIP 服务层。
 *
 * @author Shikou
 */
public interface VipService extends IService<Vip> {

    boolean redeem(HttpServletRequest request);

    Vip getVip(Long userId);

    boolean isVip(Long userId);
}
