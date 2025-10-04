package com.shikou.aicode.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.shikou.aicode.ai.model.message.*;
import com.shikou.aicode.ai.tool.BaseTool;
import com.shikou.aicode.ai.tool.ToolManager;
import com.shikou.aicode.constant.AppConstant;
import com.shikou.aicode.core.builder.VueProjectBuilder;
import com.shikou.aicode.model.entity.User;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import com.shikou.aicode.model.enums.MessageTypeEnum;
import com.shikou.aicode.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class JsonMessageStreamHandler {
    public static Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               Long appId,
                               User loginUser) {
        // 收集数据用于生成后端记忆格式
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // 用于跟踪已经见过的工具ID，判断是否是第一次调用
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds))
                .filter(StrUtil::isNotEmpty)
                .doOnComplete(() -> {
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addMessage(loginUser.getId(), appId, MessageTypeEnum.AI, aiResponse);
                    // 异步构造 Vue 项目
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + StrUtil.format("{}_{}", CodeGenTypeEnum.VUE_PROJECT.getValue(), appId);
                    VueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doOnError(error -> {
                    String errorMessage = "AI回复失败: " + error.getMessage();
                    chatHistoryService.addMessage(loginUser.getId(), appId, MessageTypeEnum.AI, errorMessage);
                });
    }

    private static String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum){
            case AI_RESPONSE -> {
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiResponseMessage.getData();
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String id = toolRequestMessage.getId();
                String name = toolRequestMessage.getName();
                BaseTool tool = ToolManager.getTool(name);
                if(id!=null && !seenToolIds.contains(id) && tool!=null){
                    seenToolIds.add(id);
                    return tool.getToolRequestResponse();
                }
                return "";
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String name = toolExecutedMessage.getName();
                BaseTool tool = ToolManager.getTool(name);
                JSONObject arguments = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String result = tool.getToolExecutedResult(arguments);
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型: {}", typeEnum);
                return "";
            }
        }
    }
}
