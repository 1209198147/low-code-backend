package com.shikou.aicode.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.shikou.aicode.model.dto.invitationcode.InvitationCodeQueryRequest;
import com.shikou.aicode.model.entity.InvitationCode;
import com.shikou.aicode.model.vo.InvitationCodeVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 邀请码 服务层。
 *
 * @author Shikou
 */
public interface InvitationCodeService extends IService<InvitationCode> {

    String generateInvitationCode(HttpServletRequest request);

    boolean verifyCode(String code);

    boolean useCode(Long userId, String code);

    List<InvitationCodeVO> listInvitationCodeVO(HttpServletRequest request);

    List<InvitationCodeVO> getInvitationCodeVOList(List<InvitationCode> list);

    QueryWrapper getQueryWrapper(InvitationCodeQueryRequest queryRequest);
}
