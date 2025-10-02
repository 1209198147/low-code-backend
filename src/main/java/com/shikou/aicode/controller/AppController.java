package com.shikou.aicode.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.shikou.aicode.annotation.AuthCheck;
import com.shikou.aicode.common.BaseResponse;
import com.shikou.aicode.common.DeleteRequest;
import com.shikou.aicode.common.ResultUtils;
import com.shikou.aicode.constant.AppConstant;
import com.shikou.aicode.constant.UserConstant;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.exception.ThrowUtils;
import com.shikou.aicode.model.dto.app.AppAddRequest;
import com.shikou.aicode.model.dto.app.AppAdminUpdateRequest;
import com.shikou.aicode.model.dto.app.AppQueryRequest;
import com.shikou.aicode.model.dto.app.AppUpdateRequest;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import com.shikou.aicode.model.vo.AppVO;
import com.shikou.aicode.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.shikou.aicode.model.entity.App;
import com.shikou.aicode.service.AppService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author Shikou
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;
    @Resource
    private UserService userService;

    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId,
                                                       @RequestParam String message,
                                                       HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 id 错误");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "提示词不能为空");
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 调用服务生成代码（SSE 流式返回）
        Flux<String> contentFlux = appService.chatToGenCode(appId, message, loginUser);
        return contentFlux
                .map(chunk -> {
                    Map<String, String> wrapper = Map.of("d", chunk);
                    String jsonData = JSONUtil.toJsonStr(wrapper);
                    return ServerSentEvent.<String>builder()
                            .data(jsonData)
                            .build();
                })
                .concatWith(Mono.just(
                        // 发送结束事件
                        ServerSentEvent.<String>builder()
                                .event("done")
                                .data("")
                                .build()
                ));
    }

    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");

        User loginUser = userService.getLoginUser(request);
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称暂时为 initPrompt 前 12 位
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 12)));
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateRequest appUpdateRequest, HttpServletRequest request) {
        if (appUpdateRequest == null || appUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = appUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 判断是否是本人
        if (!oldApp.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        App app = new App();
        app.setId(id);
        app.setAppName(appUpdateRequest.getAppName());
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldApp.getUserId().equals(loginUser.getId()) && !UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/vo")
    public BaseResponse<AppVO> getAppVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类（包含用户信息）
        return ResultUtils.success(appService.getAppVO(app));
    }

    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    @PostMapping("/good/list/page/vo")
    public BaseResponse<Page<AppVO>> listGoodAppVOByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 限制每页最多 20 个
        long pageSize = appQueryRequest.getPageSize();
        ThrowUtils.throwIf(pageSize > 20, ErrorCode.PARAMS_ERROR, "每页最多查询 20 个应用");
        long pageNum = appQueryRequest.getPageNum();
        // 只查询精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        // 分页查询
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteAppByAdmin(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = appService.removeById(id);
        return ResultUtils.success(result);
    }

    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateAppByAdmin(@RequestBody AppAdminUpdateRequest appAdminUpdateRequest) {
        if (appAdminUpdateRequest == null || appAdminUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = appAdminUpdateRequest.getId();
        // 判断是否存在
        App oldApp = appService.getById(id);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateRequest, app);
        // 设置编辑时间
        app.setEditTime(LocalDateTime.now());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> listAppVOByPageByAdmin(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), queryWrapper);
        // 数据封装
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    @GetMapping("/admin/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> getAppVOByIdByAdmin(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(appService.getAppVO(app));
    }
}
