package com.shikou.aicode.mq.producer;

import com.shikou.aicode.mq.model.ScreenShotMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ScreenShotMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送截图消息到队列
     * @param url 要截图的URL
     * @param appId 应用ID
     */
    public void sendScreenShotMessage(String url, Long appId) {
        try {
            ScreenShotMessage message = new ScreenShotMessage();
            message.setUrl(url);
            message.setAppId(appId);

            // 发送消息到交换机
            rabbitTemplate.convertAndSend("screenShotExchange", "screenShot", message);

            log.info("截图消息发送成功, appId: {}, url: {}", appId, url);
        } catch (Exception e) {
            log.error("截图消息发送失败, appId: {}, url: {}", appId, url, e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    /**
     * 发送截图消息到队列（带回调）
     * @param url 要截图的URL
     * @param appId 应用ID
     * @return 发送是否成功
     */
    public boolean sendScreenShotMessageWithConfirm(String url, Long appId) {
        try {
            ScreenShotMessage message = new ScreenShotMessage();
            message.setUrl(url);
            message.setAppId(appId);

            // 设置确认回调
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    log.info("消息确认成功, appId: {}, url: {}", appId, url);
                } else {
                    log.error("消息确认失败, appId: {}, url: {}, cause: {}", appId, url, cause);
                }
            });

            // 发送消息
            CorrelationData correlationData = new CorrelationData(appId + ":" + url);
            rabbitTemplate.convertAndSend("screenShotExchange", "screenShot", message, correlationData);

            return true;
        } catch (Exception e) {
            log.error("截图消息发送失败, appId: {}, url: {}", appId, url, e);
            return false;
        }
    }
}
