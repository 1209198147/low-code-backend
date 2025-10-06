package com.shikou.aicode.constant;

/**
 * 用户常量
 */
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";
    
    // endregion

    String ATTENDANCE_KEY = "user:attendance:";

    // 签到获取的硬币数
    Integer ATTENDANCE_COIN = 1;
    // 连续签到获取的硬币数
    Integer CONTINUOUS_ATTENDANCE_COIN = 5;
    // 购买VIP花费的硬币数
    Integer VIP_REDEMPTION_COST = 100;
    // 购买的VIP天数
    Integer VIP_REDEMPTION_DAYS = 7;
    // 普通用户每日对话次数
    int COMMON_USER_CHAT_TIME = 20;
    // VIP用户每日对话次数
    int VIP_USER_CHAT_TIME = 50;
    // 用户可以申请的邀请码最大数量
    int MAX_INVITATION_CODE_NUM = 5;
    // 用户申请邀请码花费的硬币数
    int INVITATION_CODE_COST = 50;
}