package com.shikou.aicode.core.handler;

import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import com.shikou.aicode.service.ChatHistoryService;
import reactor.core.publisher.Flux;

public class StreamMessageExecutor {
    public static Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  Long appId,
                                  User loginUser,
                                  CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case VUE_PROJECT ->
                    JsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE ->
                    SimpleTextStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}
