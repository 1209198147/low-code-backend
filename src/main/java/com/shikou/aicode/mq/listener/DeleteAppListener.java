package com.shikou.aicode.mq.listener;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.shikou.aicode.constant.AppConstant;
import com.shikou.aicode.model.entity.App;
import com.shikou.aicode.mq.model.DeleteAppMessage;
import com.shikou.aicode.service.AppService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
public class DeleteAppListener {
    @Resource
    private AppService appService;

    private final String OUTPUT_CODE_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;
    private final String DEPLOY_CODE_DIR = AppConstant.CODE_DEPLOY_ROOT_DIR;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private List<Long> appIds = new ArrayList<>();
    private final long waitTime = 1;
    private ReentrantLock lock = new ReentrantLock();

    @RabbitListener(queues = "deleteAppQueue", ackMode = "MANUAL")
    public void receiveAppDeleteMessage(Message message, Channel channel){
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try{
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("收到删除应用消息 {}", messageBody);
            DeleteAppMessage deleteAppMessage = JSONUtil.toBean(messageBody, DeleteAppMessage.class);
            Long appId = deleteAppMessage.getAppId();
            if(appIds.isEmpty()){
                executor.scheduleWithFixedDelay(()->{
                    try{
                        lock.lock();
                        cleanUpCodeOutputFile(appIds);
                    }finally {
                        lock.unlock();
                    }
                }, waitTime, waitTime, TimeUnit.HOURS);
            }
            // 聚合消息
            boolean canAdd = lock.tryLock(10, TimeUnit.MILLISECONDS);
            // 如果获取锁失败代表上一批已经在执行，所以要新开一批
            if(!canAdd){
                appIds = new ArrayList<>();
            }
            appIds.add(appId);
            // 如果拿到锁了要释放
            if(lock.isHeldByCurrentThread()){
                lock.unlock();
            }
            channel.basicAck(deliveryTag, false);
        }catch (Exception e){
            log.error("添加删除应用任务失败 {}", e.getMessage(), e);
            try {
                channel.basicReject(deliveryTag, true);
                log.warn("消息处理失败, deliveryTag: {}", deliveryTag);
            } catch (IOException ioException) {
                log.error("拒绝消息时发生IO异常, deliveryTag: {}", deliveryTag, ioException);
            }
        }

    }

    private void cleanUpCodeOutputFile(List<Long> appIds){
        StopWatch totalStopWatch = new StopWatch();
        log.info("开始清理生成的应用项目文件");
        totalStopWatch.start();
        List<App> deletAppList = appService.getList(appIds, true);
        if(CollectionUtil.isNotEmpty(deletAppList)){
            List<String> deleteAppDirPaths = deletAppList.stream().map(deletApp ->
                    OUTPUT_CODE_DIR + File.separator
                            + StrUtil.format("{}_{}", deletApp.getCodeGenType(), deletApp.getId())
            ).toList();

            List<String> deleteAppDeployDirPaths = deletAppList.stream()
                    .filter(deletApp -> StringUtils.isNotBlank(deletApp.getDeployKey()))
                    .map(deletApp -> DEPLOY_CODE_DIR + File.separator + deletApp.getDeployKey())
                    .toList();
            StopWatch stopWatch = new StopWatch();

            // 删除生成的应用项目目录
            log.info("开始删除output文件");
            stopWatch.start();
            deleteAppDirPaths.stream().forEach(path ->{
                if(FileUtil.exist(path)){
                    try{
                        boolean result = FileUtil.del(path);
                        if(result){
                            log.info("删除 {} 成功", path);
                        }else{
                            log.info("删除 {} 失败", path);
                        }
                    }catch (Exception e){
                        log.error("删除 {} 报错", path);
                    }
                }
            });
            stopWatch.stop();
            log.info("删除output文件 花费: {}ms", stopWatch.getTotalTimeMillis());
            // 删除部署的应用项目目录
            log.info("开始删除deploy文件");
            stopWatch.start();
            deleteAppDeployDirPaths.stream().forEach(path ->{
                if(FileUtil.exist(path)){
                    try{
                        boolean result = FileUtil.del(path);
                        if(result){
                            log.info("删除 {} 成功", path);
                        }else{
                            log.info("删除 {} 失败", path);
                        }
                    }catch (Exception e){
                        log.error("删除 {} 报错", path);
                    }
                }
            });
            stopWatch.stop();
            log.info("开始删除deploy文件 花费: {}ms", stopWatch.getTotalTimeMillis());
        }
        totalStopWatch.stop();
        log.info("本次清理生成的应用项目文件 花费: {}ms", totalStopWatch.getTotalTimeMillis());
    }
}
