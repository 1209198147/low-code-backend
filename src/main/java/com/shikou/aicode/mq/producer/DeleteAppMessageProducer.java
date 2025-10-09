package com.shikou.aicode.mq.producer;

import com.shikou.aicode.mq.model.DeleteAppMessage;
import com.shikou.aicode.mq.model.ScreenShotMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class DeleteAppMessageProducer {
    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送删除消息到队列
     * @param appId 应用ID
     */
    public void sendDeleteAppMessage(Long appId) {
        try {
            DeleteAppMessage message = new DeleteAppMessage();
            message.setAppId(appId);

            // 发送消息到交换机
            rabbitTemplate.convertAndSend("exchange", "deleteApp", message);

            log.info("删除应用消息发送成功, appId: {}", appId);
        } catch (Exception e) {
            log.error("删除应用消息发送失败, appId: {}", appId, e);
            throw new RuntimeException("消息发送失败", e);
        }
    }

    /**
     * 发送截图消息到队列（带回调）
     * @param appId 应用ID
     * @return 发送是否成功
     */
    public boolean sendDeleteAppMessageWithConfirm(Long appId) {
        try {
            DeleteAppMessage message = new DeleteAppMessage();
            message.setAppId(appId);

            // 设置确认回调
            rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
                if (ack) {
                    log.info("消息确认成功, appId: {}", appId);
                } else {
                    log.error("消息确认失败, appId: {}, cause: {}", appId, cause);
                }
            });

            // 发送消息
            CorrelationData correlationData = new CorrelationData(appId.toString() );
            rabbitTemplate.convertAndSend("exchange", "deleteApp", message, correlationData);

            return true;
        } catch (Exception e) {
            log.error("删除应用消息发送失败, appId: {}", appId, e);
            return false;
        }
    }
}
