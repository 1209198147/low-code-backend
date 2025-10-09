package com.shikou.aicode.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import com.shikou.aicode.annotation.AuthCheck;
import com.shikou.aicode.common.BaseResponse;
import com.shikou.aicode.common.DeleteRequest;
import com.shikou.aicode.common.ResultUtils;
import com.shikou.aicode.constant.UserConstant;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.dto.user.*;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.vo.InvitationCodeVO;
import com.shikou.aicode.model.vo.LoginUserVO;
import com.shikou.aicode.model.vo.UserVO;
import com.shikou.aicode.service.InvitationCodeService;
import com.shikou.aicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * 用户 控制层。
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private InvitationCodeService invitationCodeService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest 用户注册请求
     * @return 注册结果
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String code = userRegisterRequest.getCode();
        long result = userService.userRegister(userAccount, userPassword, checkPassword, code);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 游客登入
     *
     * @param request          请求对象
     * @return 脱敏后的用户登录信息
     */
    @PostMapping("/guest/login")
    public BaseResponse<LoginUserVO> guestLogin(HttpServletRequest request) {
        LoginUserVO loginUserVO = userService.guestLogin(request);
        return ResultUtils.success(loginUserVO);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     *
     * @param request 请求对象
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @PostMapping("/changePassword")
    @AuthCheck
    public BaseResponse<Boolean> changePassword(@RequestBody UserChangePasswordRequest changePasswordRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String newPassword = changePasswordRequest.getNewPassword();
        String oldPassword = changePasswordRequest.getOldPassword();
        ThrowUtils.throwIf(StringUtils.isAnyBlank(oldPassword, newPassword), ErrorCode.PARAMS_ERROR, "旧密码和新密码不能为空");
        ThrowUtils.throwIf(oldPassword.equals(newPassword), ErrorCode.PARAMS_ERROR, "旧密码和新密码不能相同");
        if (newPassword.length() < 8 || newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新的密码长度过短");
        }

        User loginUser = userService.getLoginUser(request);
        String password = loginUser.getUserPassword();
        oldPassword = userService.getEncryptPassword(oldPassword);
        ThrowUtils.throwIf(!password.equals(oldPassword), ErrorCode.PARAMS_ERROR, "旧的密码不正确");
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setUserPassword(userService.getEncryptPassword(newPassword));
        boolean result = userService.updateById(updateUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/modify")
    @AuthCheck
    public BaseResponse<LoginUserVO> userModify(@RequestBody UserModifyRequest modifyRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(modifyRequest==null || request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String userName = modifyRequest.getUserName();
        String userProfile = modifyRequest.getUserProfile();
        if(StringUtils.isAllBlank(userName, userProfile)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改数据为空");
        }
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        if(StringUtils.isNotBlank(userName)){
            ThrowUtils.throwIf(userName.length()>10, ErrorCode.PARAMS_ERROR, "用户名不能超过10个字符");
            updateUser.setUserName(userName);
        }
        if(StringUtils.isNotBlank(userProfile)){
            ThrowUtils.throwIf(userProfile.length()>512, ErrorCode.PARAMS_ERROR, "个人简介不能超过512个字符");
            updateUser.setUserProfile(userProfile);
        }
        boolean result = userService.updateById(updateUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "修改个人信息失败");
        loginUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
        return ResultUtils.success(loginUserVO);
    }

    @PostMapping("/avatar/upload")
    @AuthCheck
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(file==null || request == null, ErrorCode.PARAMS_ERROR);
        String avatarUrl = userService.updateUserAvatar(file, request);
        return ResultUtils.success(avatarUrl);
    }

    @GetMapping("/get/invitationCode")
    @AuthCheck
    public BaseResponse<String> getInvitationCode(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        String code = invitationCodeService.generateInvitationCode(request);
        return ResultUtils.success(code);
    }

    @GetMapping("/list/invitationCode")
    @AuthCheck
    public BaseResponse<List<InvitationCodeVO>> listInvitationCode(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        List<InvitationCodeVO> invitationCodeVOList = invitationCodeService.listInvitationCodeVO(request);
        return ResultUtils.success(invitationCodeVOList);
    }

    @PostMapping("/attendance")
    public BaseResponse<Boolean> userAttendance(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean attendanced = userService.attendance(request);
        return ResultUtils.success(attendanced);
    }

    @GetMapping("/list/attendance")
    public BaseResponse<List<Integer>> getAttendance(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String key = UserConstant.ATTENDANCE_KEY + LocalDate.now().getYear() + ":" + loginUser.getId();
        RBitSet bitSet = redissonClient.getBitSet(key);
        BitSet bs = bitSet.asBitSet();
        List<Integer> attendanceList = new ArrayList<>();
        bs.stream().forEach(attendanceList::add);
        return ResultUtils.success(attendanceList);
    }

    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
