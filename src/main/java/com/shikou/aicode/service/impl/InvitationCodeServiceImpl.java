package com.shikou.aicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.shikou.aicode.constant.UserConstant;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.dto.invitationcode.InvitationCodeQueryRequest;
import com.shikou.aicode.model.entity.InvitationCode;
import com.shikou.aicode.mapper.InvitationCodeMapper;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.vo.InvitationCodeVO;
import com.shikou.aicode.model.vo.UserVO;
import com.shikou.aicode.service.InvitationCodeService;
import com.shikou.aicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 邀请码 服务层实现。
 *
 * @author Shikou
 */
@Service
@Slf4j
public class InvitationCodeServiceImpl extends ServiceImpl<InvitationCodeMapper, InvitationCode>  implements InvitationCodeService {
    @Lazy
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String generateInvitationCode(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        QueryWrapper queryWrapper = QueryWrapper.create().select(InvitationCode::getId).eq(InvitationCode::getUserId, userId);
        long count = count(queryWrapper);
        if(count >= UserConstant.MAX_INVITATION_CODE_NUM){
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "无法申请更多的邀请码了");
        }
        boolean reduced = userService.reduceCoin(userId, UserConstant.INVITATION_CODE_COST);
        ThrowUtils.throwIf(!reduced, ErrorCode.OPERATION_ERROR, "你的硬币不足以兑换邀请码");
        InvitationCode invitationCode = new InvitationCode();
        invitationCode.setUserId(userId);
        String code = RandomUtil.randomString(8);
        invitationCode.setCode(code);
        boolean saved = save(invitationCode);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "兑换邀请码失败");
        return code;
    }

    @Override
    public boolean verifyCode(String code){
        QueryWrapper queryWrapper = QueryWrapper.create().eq(InvitationCode::getCode, code);
        InvitationCode invitationCode = getOne(queryWrapper);
        if(invitationCode == null || invitationCode.getInviteeId() != null){
            return false;
        }
        return true;
    }

    @Override
    public boolean useCode(Long userId, String code){
        String lockKey = "invitation_code:"+code;
        // 使用分布式锁，防止多个请求同时使用同一个邀请码
        RLock rLock = redissonClient.getLock(lockKey);
        try{
            if (rLock.tryLock(0, -1, TimeUnit.SECONDS)){
                User user = userService.getById(userId);
                ThrowUtils.throwIf(user==null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
                QueryWrapper queryWrapper = QueryWrapper.create().eq(InvitationCode::getCode, code);
                InvitationCode invitationCode = getOne(queryWrapper);
                if(invitationCode == null || invitationCode.getInviteeId()!=null){
                    throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "验证码错误或已被使用");
                }
                invitationCode.setInviteeId(userId);
                return updateById(invitationCode);
            }
            return false;
        }catch (Exception e){
            log.error("使用邀请码失败 {}", e.getMessage(), e);
            return false;
        }
        finally {
            if(rLock.isHeldByCurrentThread()){
                rLock.unlock();
            }
        }
    }

    @Override
    public List<InvitationCodeVO> listInvitationCodeVO(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
        Long userId = loginUser.getId();
        QueryWrapper queryWrapper = QueryWrapper.create().eq(InvitationCode::getUserId, userId);
        List<InvitationCode> list = list(queryWrapper);
        return getInvitationCodeVOList(list);
    }

    @Override
    public List<InvitationCodeVO> getInvitationCodeVOList(List<InvitationCode> list){
        Set<Long> userIds = new HashSet<>();
        list.stream().forEach(invitationCode -> {
            userIds.add(invitationCode.getUserId());
            Long inviteeId = invitationCode.getInviteeId();
            if(inviteeId!=null){
                userIds.add(inviteeId);
            }
        });
        List<User> users = userService.listByIds(userIds);
        Map<Long, UserVO> userMap = users.stream().collect(Collectors.toMap(User::getId, userService::getUserVO));

        List<InvitationCodeVO> invitationCodeVOList = new ArrayList<>();
        list.stream().forEach(invitationCode -> {
            InvitationCodeVO invitationCodeVO = new InvitationCodeVO();
            BeanUtil.copyProperties(invitationCode, invitationCodeVO);
            UserVO createUser = userMap.get(invitationCode.getUserId());
            invitationCodeVO.setCreateUser(createUser);
            Long inviteeId = invitationCode.getInviteeId();
            if(inviteeId!=null){
                UserVO invitee = userMap.get(invitationCode.getInviteeId());
                invitationCodeVO.setInvitee(invitee);
                invitationCodeVO.setUsed(true);
            }
            invitationCodeVOList.add(invitationCodeVO);
        });
        return invitationCodeVOList;
    }

    @Override
    public QueryWrapper getQueryWrapper(InvitationCodeQueryRequest queryRequest){
        QueryWrapper queryWrapper = new QueryWrapper();
        Long id = queryRequest.getId();
        if(id!=null){
            queryWrapper.eq(InvitationCode::getId, id);
        }
        String code = queryRequest.getCode();
        if(StringUtils.isNotBlank(code)){
            queryWrapper.like(InvitationCode::getCode, code);
        }
        Long userId = queryRequest.getUserId();
        if(userId!=null){
            queryWrapper.eq(InvitationCode::getUserId, userId);
        }
        Long inviteeId = queryRequest.getInviteeId();
        if(inviteeId!=null){
            queryWrapper.eq(InvitationCode::getInviteeId, inviteeId);
        }
        return queryWrapper;
    }
}
