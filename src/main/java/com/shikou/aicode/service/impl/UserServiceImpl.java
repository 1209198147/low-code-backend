package com.shikou.aicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.shikou.aicode.constant.UserConstant;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.entity.InvitationCode;
import com.shikou.aicode.model.entity.Vip;
import com.shikou.aicode.model.vo.LoginUserVO;
import com.shikou.aicode.model.vo.UserVO;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.model.dto.user.UserQueryRequest;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.mapper.UserMapper;
import com.shikou.aicode.model.enums.UserRoleEnum;
import com.shikou.aicode.service.InvitationCodeService;
import com.shikou.aicode.service.UserService;
import com.shikou.aicode.service.VipService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.shikou.aicode.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private VipService vipService;
    @Resource
    private InvitationCodeService invitationCodeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long userRegister(String userAccount, String userPassword, String checkPassword, String code) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 判断邀请码是否正确或被使用
        boolean vailed = invitationCodeService.verifyCode(code);
        ThrowUtils.throwIf(!vailed, ErrorCode.NOT_FOUND_ERROR, "邀请码错误或已被使用");

        // 3. 查询用户是否已存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 4. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 5. 创建用户，插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败，数据库错误");
        }
        // 6. 使用邀请码
        invitationCodeService.useCode(user.getId(), code);
        return user.getId();
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        Vip vip = vipService.getVip(user.getId());
        if(vip!=null){
            LocalDateTime expiredTime = vip.getExpiredTime();
            loginUserVO.setIsVip(LocalDateTime.now().isBefore(expiredTime));
            loginUserVO.setVipExpiredTime(expiredTime);
        }else{
            loginUserVO.setIsVip(false);
        }
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验参数
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 4. 如果用户存在，记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 5. 返回脱敏的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public boolean attendance(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = getLoginUser(request);
        LocalDate today = LocalDate.now();
        int index = today.getDayOfYear();

        String key = UserConstant.ATTENDANCE_KEY + today.getYear() + ":" + loginUser.getId();
        RBitSet bitSet = redissonClient.getBitSet(key);
        BitSet bs = bitSet.asBitSet();
        if(bs.get(index)){
            return true;
        }
        bitSet.set(index);
        int continuousDays = 0;
        for (int i = index; i >= 0; i--) {
            if (bs.get(i)) {
                continuousDays++;
            } else {
                break;
            }
        }
        int addCoinNum = UserConstant.ATTENDANCE_COIN;
        if(continuousDays >= 7){
            addCoinNum = UserConstant.CONTINUOUS_ATTENDANCE_COIN;
        }
        increaseCoin(loginUser.getId(), addCoinNum);
        bitSet.expire(Duration.ofDays(today.lengthOfYear()-index+1));
        return true;
    }

    @Override
    public boolean increaseCoin(Long id, int num){
        QueryWrapper queryWrapper = QueryWrapper.create().select(User::getCoin).eq(User::getId, id);
        User updateUser = getOne(queryWrapper);
        updateUser.setId(id);
        updateUser.setCoin(updateUser.getCoin() + num);
        return updateById(updateUser);
    }

    @Override
    public boolean reduceCoin(Long id, int num){
        QueryWrapper queryWrapper = QueryWrapper.create().select(User::getCoin).eq(User::getId, id);
        User updateUser = getOne(queryWrapper);
        if(updateUser.getCoin() < num){
            return false;
        }
        updateUser.setId(id);
        updateUser.setCoin(updateUser.getCoin() - num);
        return updateById(updateUser);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询当前用户信息
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "用户未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id) // where id = ${id}
                .eq("userRole", userRole) // and userRole = ${userRole}
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "shikou";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes(StandardCharsets.UTF_8));
    }
}
