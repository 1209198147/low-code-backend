package com.shikou.aicode.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.shikou.aicode.constant.AppConstant;
import com.shikou.aicode.core.AiGeneratorFacade;
import com.shikou.aicode.core.builder.VueProjectBuilder;
import com.shikou.aicode.core.handler.StreamMessageExecutor;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.dto.app.AppQueryRequest;
import com.shikou.aicode.model.entity.App;
import com.shikou.aicode.mapper.AppMapper;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import com.shikou.aicode.model.enums.MessageTypeEnum;
import com.shikou.aicode.model.vo.AppVO;
import com.shikou.aicode.model.vo.UserVO;
import com.shikou.aicode.service.AppService;
import com.shikou.aicode.service.ChatHistoryService;
import com.shikou.aicode.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
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
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{
    @Resource
    private UserService userService;
    @Resource
    private AiGeneratorFacade aiGeneratorFacade;
    @Resource
    private ChatHistoryService chatHistoryService;

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
        // 添加用户消息
        chatHistoryService.addMessage(loginUser.getId(), appId, MessageTypeEnum.USER, message);
        Flux<String> stream = aiGeneratorFacade.generateCodeAndSave(message, codeGenTypeEnum, appId);
        // 保存AI回复
        return StreamMessageExecutor.doExecute(stream, chatHistoryService, appId, loginUser, codeGenTypeEnum);
    }

    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验，仅本人可以部署自己的应用
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限部署该应用");
        }
        // 4. 判断是否有部署密钥
        String deployKey = app.getDeployKey();
        if( StringUtils.isBlank(deployKey) ){
            deployKey = RandomUtil.randomString(6);
        }
        // 5.部署
        try {
            String sourcePath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + StrUtil.format("{}_{}", app.getCodeGenType(), appId);
            File sourceDir = new File(sourcePath);
            ThrowUtils.throwIf(!FileUtil.exist(sourceDir), ErrorCode.NOT_FOUND_ERROR, "应用未生成,请先生成应用");
            String deployPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
            File deployDir = new File(deployPath);
            CodeGenTypeEnum typeEnum = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
            if(CodeGenTypeEnum.VUE_PROJECT.equals(typeEnum)){
                boolean success = VueProjectBuilder.buildProject(sourcePath);
                ThrowUtils.throwIf(!success, ErrorCode.SYSTEM_ERROR, "vue项目构建失败");
                File distDir = new File(sourcePath, "dist");
                if(!distDir.exists()){
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "构建完成，但未能找到dist文件夹");
                }
                sourceDir = distDir;
                log.info("vue项目构建成功，部署dist文件夹: {}", distDir.getAbsolutePath());
            }
            FileUtil.copyContent(sourceDir, deployDir, true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用部署失败：" + e.getMessage());
        }
        // 6.更新数据库
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean result = updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        return String.format("%s/%s", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    @Override
    public boolean removeById(Serializable id) {
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR, "id不能为空");
        Long appId = (Long) id;
        if(appId < 1){
            return false;
        }
        try{
            chatHistoryService.deleteByAppId(appId);
        }catch (Exception e){
            log.error("删除应用对应历史对话记录失败 {}", e.getMessage());
        }
        return super.removeById(id);
    }
}
