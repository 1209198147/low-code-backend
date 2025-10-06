package com.shikou.aicode.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import java.io.Serializable;
import java.time.LocalDateTime;

import java.io.Serial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邀请码 实体类。
 *
 * @author Shikou
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("invitation_code")
public class InvitationCode implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 邀请码
     */
    private String code;

    /**
     * 被邀请用户ID
     */
    @Column("inviteeId")
    private Long inviteeId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
