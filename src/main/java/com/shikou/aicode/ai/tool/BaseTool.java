package com.shikou.aicode.ai.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;

public abstract class BaseTool {
    public abstract String getToolName();

    public abstract String getDisplayName();

    public String getToolRequestResponse(){
        return StrUtil.format("\n\n[选择工具]{}\n\n", getDisplayName());
    }

    public abstract String getToolExecutedResult(JSONObject arguments);
}
