package com.shikou.aicode.model.vo;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class InvitationCodeVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private UserVO createUser;

    private String code;

    private boolean isUsed = false;

    private UserVO invitee;

    private LocalDateTime createTime;
}
