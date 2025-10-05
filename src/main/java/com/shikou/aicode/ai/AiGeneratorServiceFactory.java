package com.shikou.aicode.ai;

import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shikou.aicode.ai.tool.ToolManager;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import com.shikou.aicode.service.ChatHistoryService;
import com.shikou.aicode.utils.SpringContextUtil;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class AiGeneratorServiceFactory {
    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;
    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;
    @Resource
    private ChatHistoryService chatHistoryService;
    @Resource
    private ToolManager toolManager;

    private final Cache<String, AiGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) -> {
                log.debug("AI Service被移除 appId：{} 原因：{}", key, cause);
            }).build();

    public AiGeneratorService getAiGeneratorService(Long appId, CodeGenTypeEnum typeEnum){
        String cacheKey = getkey(appId, typeEnum);
        return serviceCache.get(cacheKey, key -> createAiCodeGeneratorService(appId, typeEnum));
    }

    public AiGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum typeEnum) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .chatMemoryStore(redisChatMemoryStore)
                .maxMessages(20)
                .build();
        chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);

        return switch (typeEnum){
            case HTML, MULTI_FILE -> {
                // 使用多例解决并发问题
                StreamingChatModel streamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(streamingChatModel)
                        .chatMemory(chatMemory)
                        .build();
            }
            case VUE_PROJECT -> {
                // 使用多例解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield  AiServices.builder(AiGeneratorService.class)
                        .chatModel(chatModel)
                        .streamingChatModel(reasoningStreamingChatModel)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getTools())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage.from(
                                toolExecutionRequest, "Erorr: there is no tool called " + toolExecutionRequest.name()
                        ))
                        .build();
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成类型");
        };
    }

    private String getkey(Long appId, CodeGenTypeEnum typeEnum){
        return StrUtil.format("{}_{}", typeEnum.getValue(), appId);
    }
}
