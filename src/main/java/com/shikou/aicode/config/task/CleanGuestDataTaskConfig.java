package com.shikou.aicode.config.task;

import cn.hutool.core.collection.CollectionUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.shikou.aicode.model.entity.App;
import com.shikou.aicode.model.entity.ChatHistory;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.UserRoleEnum;
import com.shikou.aicode.service.AppService;
import com.shikou.aicode.service.ChatHistoryService;
import com.shikou.aicode.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class CleanGuestDataTaskConfig {
    @Resource
    private UserService userService;
    @Resource
    private AppService appService;
    @Resource
    private ChatHistoryService chatHistoryService;

    @Scheduled(cron = "0 0 3 * * 0")
    public void cleanUpGuestData(){
        log.info("开始清理游客数据");
        QueryWrapper queryWrapper = QueryWrapper.create().eq(User::getUserRole, UserRoleEnum.GUEST.getValue()).select(User::getId);
        List<User> list = userService.list(queryWrapper);

        if(CollectionUtil.isNotEmpty(list)){
            List<Long> guestIds = list.stream().map(User::getId).collect(Collectors.toList());
            queryWrapper = QueryWrapper.create().in(App::getUserId, guestIds).select(App::getId);
            List<Long> appIds = appService.list(queryWrapper).stream().map(App::getId).collect(Collectors.toList());

            boolean result = appService.removeByIds(appIds);
            if(result){
                log.info("清理游客应用数据成功");
            }else {
                log.error("清理游客应用数据失败");
            }
            queryWrapper = QueryWrapper.create().in(ChatHistory::getAppId, appIds);
            result = chatHistoryService.remove(queryWrapper);
            if(result){
                log.info("清理游客应用对话历史数据成功");
            }else {
                log.error("清理游客应用对话历史数据失败");
            }
        }
    }
}
