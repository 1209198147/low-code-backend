package com.shikou.aicode.mq.listener;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.shikou.aicode.mq.model.ScreenShotMessage;
import com.shikou.aicode.service.AppService;
import com.shikou.aicode.service.ScreenShotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class ScreenShotListener {
    @Resource
    private ScreenShotService screenShotService;
    @Resource
    private AppService appService;

    @RabbitListener(queues = "screenShotQueue", ackMode = "MANUAL")
    public void receiveScreenShotMessage(Message message, Channel channel){
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try{
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("收到截图消息 {}", messageBody);
            ScreenShotMessage screenShotMessage = JSONUtil.toBean(messageBody, ScreenShotMessage.class);
            Long appId = screenShotMessage.getAppId();
            String url = screenShotMessage.getUrl();
            String cosUrl = screenShotService.generateAndUploadScreenshot(appId, url);
            appService.updateCover(appId, cosUrl);
            channel.basicAck(deliveryTag, false);
        }catch (Exception e){
            log.error("生成截图失败 {}", e.getMessage(), e);
            try {
                boolean requeue = shouldRequeue(e);
                channel.basicReject(deliveryTag, requeue);
                log.warn("消息处理失败, deliveryTag: {}, requeue: {}", deliveryTag, requeue);
            } catch (IOException ioException) {
                log.error("拒绝消息时发生IO异常, deliveryTag: {}", deliveryTag, ioException);
            }
        }

    }

    private boolean shouldRequeue(Exception e) {
        return e instanceof TimeoutException ||
                e instanceof IOException ||
                e instanceof RuntimeException;
    }
}
