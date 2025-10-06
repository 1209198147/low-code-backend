package com.shikou.aicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.mybatisflex.core.paginate.Page;
import com.shikou.aicode.annotation.AuthCheck;
import com.shikou.aicode.common.BaseResponse;
import com.shikou.aicode.common.DeleteRequest;
import com.shikou.aicode.common.ResultUtils;
import com.shikou.aicode.constant.UserConstant;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.dto.invitationcode.InvitationCodeQueryRequest;
import com.shikou.aicode.model.dto.user.UserAddRequest;
import com.shikou.aicode.model.entity.InvitationCode;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.vo.InvitationCodeVO;
import com.shikou.aicode.service.InvitationCodeService;
import com.shikou.aicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 邀请码 控制层。
 *
 * @author Shikou
 */
@RestController
@RequestMapping("/invitationCode")
public class InvitationCodeController {

    @Resource
    private InvitationCodeService invitationCodeService;
    @Resource
    private UserService userService;

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> addCode(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setUserId(userId);
        String code = RandomUtil.randomString(8);
        invitationCode.setCode(code);
        boolean saved = invitationCodeService.save(invitationCode);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "申请邀请码失败");
        return ResultUtils.success(code);
    }

    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<InvitationCodeVO>> listUserVOByPage(@RequestBody InvitationCodeQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = queryRequest.getPageNum();
        long pageSize = queryRequest.getPageSize();
        Page<InvitationCode> invitationCodePage = invitationCodeService.page(Page.of(pageNum, pageSize),
                invitationCodeService.getQueryWrapper(queryRequest));
        // 数据脱敏
        Page<InvitationCodeVO> invitationCodeVOPage = new Page<>(pageNum, pageSize, invitationCodePage.getTotalRow());
        List<InvitationCodeVO> userVOList = invitationCodeService.getInvitationCodeVOList(invitationCodePage.getRecords());
        invitationCodeVOPage.setRecords(userVOList);
        return ResultUtils.success(invitationCodeVOPage);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = invitationCodeService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

}
