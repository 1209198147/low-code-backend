package com.shikou.aicode.mq.listener;

import cn.hutool.json.JSONUtil;
import com.rabbitmq.client.Channel;
import com.shikou.aicode.mq.model.ScreenShotMessage;
import com.shikou.aicode.service.AppService;
import com.shikou.aicode.service.ScreenShotService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@Component
@Slf4j
public class ScreenShotListener {
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private ScreenShotService screenShotService;
    @Resource
    private AppService appService;
    private Executor executor = Executors.newSingleThreadExecutor();

    @RabbitListener(queues = "screenShotQueue", ackMode = "MANUAL")
    public void receiveScreenShotMessage(Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        MessageProperties properties = message.getMessageProperties();

        // 获取重试次数
        Integer retryCount = (Integer) properties.getHeaders().get("x-retry-count");
        if (retryCount == null) {
            retryCount = 0;
        }

        try {
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            log.info("收到截图消息 {}, 重试次数: {}", messageBody, retryCount);

            ScreenShotMessage screenShotMessage = JSONUtil.toBean(messageBody, ScreenShotMessage.class);
            Long appId = screenShotMessage.getAppId();
            String url = screenShotMessage.getUrl();

            String cosUrl = screenShotService.generateAndUploadScreenshot(appId, url);
            appService.updateCover(appId, cosUrl);

            channel.basicAck(deliveryTag, false);
            log.info("截图处理成功, appId: {}", appId);

        } catch (Exception e) {
            log.error("生成截图失败, 重试次数: {}, 错误: {}", retryCount, e.getMessage(), e);

            try {
                if (retryCount < 3) { // 最大重试3次
                    // 重新发布到原队列，并设置延迟
                    retryWithDelay(message, channel, deliveryTag, retryCount);
                } else {
                    // 超过最大重试次数，拒绝并不重新入队
                    channel.basicReject(deliveryTag, false);
                    log.error("消息达到最大重试次数, deliveryTag: {}", deliveryTag);
                }
            } catch (IOException ioException) {
                log.error("处理重试时发生IO异常, deliveryTag: {}", deliveryTag, ioException);
            }
        }
    }

    private void retryWithDelay(Message message, Channel channel, long deliveryTag, int retryCount) throws IOException {
        // 计算延迟时间（指数退避）
        long delay = calculateDelay(retryCount);

        // 先确认原消息
        channel.basicAck(deliveryTag, false);

        log.info("消息延迟 {}ms 后重新入队, 重试次数: {}", delay, retryCount + 1);

        // 使用线程池延迟执行
        CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, executor)
                .execute(() -> {
                    try {
                        // 重新发布消息到队列
                        retryPublishMessage(message, retryCount + 1);
                    } catch (Exception e) {
                        log.error("延迟重新发布消息失败, 重试次数: {}", retryCount + 1, e);
                    }
                });
    }

    private void retryPublishMessage(Message originalMessage, int newRetryCount) {
        try {
            // 设置新的消息头
            MessageProperties newProps = new MessageProperties();
            Map<String, Object> headers = new HashMap<>();
            headers.put("x-retry-count", newRetryCount);
            headers.put("x-original-timestamp", System.currentTimeMillis());
            newProps.setHeaders(headers);

            // 创建新消息
            Message newMessage = new Message(originalMessage.getBody(), newProps);

            // 重新发布到原队列
            rabbitTemplate.convertAndSend("screenShotQueue", newMessage);

            log.info("延迟重试消息已重新发布到队列, 重试次数: {}", newRetryCount);
        } catch (Exception e) {
            log.error("重新发布消息到队列失败, 重试次数: {}", newRetryCount, e);
        }
    }

    private long calculateDelay(int retryCount) {
        // 指数退避策略：1min, 5min, 10min
        switch (retryCount) {
            case 0: return 60000L;      // 1分钟
            case 1: return 300000L;     // 5分钟
            case 2: return 600000L;     // 10分钟
            default: return 600000L;    // 默认10分钟
        }
    }
}
