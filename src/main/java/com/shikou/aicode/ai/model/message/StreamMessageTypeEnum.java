package com.shikou.aicode.ai.model.message;

import cn.hutool.core.util.ObjUtil;
import com.shikou.aicode.model.enums.MessageTypeEnum;
import lombok.Getter;

@Getter
public enum StreamMessageTypeEnum {

    AI_RESPONSE("AI响应消息", "ai_response"),
    TOOL_REQUEST("工具调用消息", "tool_request"),
    TOOL_EXECUTED("工具结果消息", "tool_executed");

    private String text;
    private String value;

    StreamMessageTypeEnum(String text, String value){
        this.text = text;
        this.value = value;
    }

    public static StreamMessageTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (StreamMessageTypeEnum anEnum : StreamMessageTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
