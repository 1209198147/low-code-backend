package com.shikou.aicode.model.dto.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserChangePasswordRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 3191241716373120793L;
    /**
     * 旧密码
     */
    private String oldPassword;
    /**
     * 新密码
     */
    private String newPassword;
}
