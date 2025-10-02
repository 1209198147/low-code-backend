package com.shikou.aicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.shikou.aicode.core.AiGeneratorFacade;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.dto.app.AppQueryRequest;
import com.shikou.aicode.model.entity.App;
import com.shikou.aicode.mapper.AppMapper;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import com.shikou.aicode.model.vo.AppVO;
import com.shikou.aicode.model.vo.UserVO;
import com.shikou.aicode.service.AppService;
import com.shikou.aicode.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author Shikou
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{
    @Resource
    private UserService userService;
    @Resource
    private AiGeneratorFacade aiGeneratorFacade;

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) return null;
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        Long userId = app.getUserId();
        if(userId != null){
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public List<AppVO> getAppVOList(List<App> records) {
        // 批量获取UserId
        Set<Long> userIds = records.stream().map(App::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> map = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return records.stream().map(record->{
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(record, appVO);
            Long userId = record.getUserId();
            appVO.setUser(map.get(userId));
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User loginUser) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该应用");
        }
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用代码生成类型错误");
        }
        return aiGeneratorFacade.generateCodeAndSave(message, codeGenTypeEnum, appId);
    }
}
