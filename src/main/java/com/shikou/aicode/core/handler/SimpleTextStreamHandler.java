package com.shikou.aicode.core.handler;

import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.MessageTypeEnum;
import com.shikou.aicode.service.ChatHistoryService;
import reactor.core.publisher.Flux;

public class SimpleTextStreamHandler {
    public static Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               Long appId,
                               User loginUser){
        StringBuilder messageBuilder = new StringBuilder();
        return originFlux.map(chunk->{
            messageBuilder.append(chunk);
            return chunk;
        }).doOnComplete(()->{
            String aiMessage = messageBuilder.toString();
            chatHistoryService.addMessage(loginUser.getId(), appId, MessageTypeEnum.AI, aiMessage);
        }).doOnError(error -> {
            // 如果 AI 回复失败，也需要保存记录到数据库中
            String errorMessage = "AI 回复失败：" + error.getMessage();
            chatHistoryService.addMessage(loginUser.getId(), appId, MessageTypeEnum.AI, errorMessage);
        });
    }
}
