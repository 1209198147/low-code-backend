package com.shikou.aicode.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserModifyRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 简介
     */
    private String userProfile;

    @Serial
    private static final long serialVersionUID = 1L;
}
