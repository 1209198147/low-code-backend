package com.shikou.aicode.ai;

import com.shikou.aicode.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

public class AiCodeGenTypeRoutingServiceFactory {

    public static AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService(){
        ChatModel chatModel = SpringContextUtil.getBean("routingAIModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }
}
