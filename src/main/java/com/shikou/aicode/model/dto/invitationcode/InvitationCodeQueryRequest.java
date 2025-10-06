package com.shikou.aicode.model.dto.invitationcode;

import com.shikou.aicode.common.PageRequest;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class InvitationCodeQueryRequest extends PageRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long userId;

    private String code;

    private Long inviteeId;
}
