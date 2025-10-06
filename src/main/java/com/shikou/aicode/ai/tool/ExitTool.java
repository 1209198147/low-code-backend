package com.shikou.aicode.ai.tool;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExitTool extends BaseTool{

    @Tool("当前任务已经完成或无需调用其他工具时，使用此工具退出操作，避免循环")
    public String exit(){
        log.info("AI 决定退出工具调用");
        return "立即停止调用工具，输出最终结果";
    }

    @Override
    public String getToolName() {
        return "exit";
    }

    @Override
    public String getDisplayName() {
        return "退出工具";
    }

    @Override
    public String getToolExecutedResult(JSONObject arguments) {
        return "\n\n[执行完成]\n\n";
    }
}
