package com.shikou.aicode.model.dto.mq;

import lombok.Data;

@Data
public class ScreenShotMessage {
    private String url;
    private Long appId;
}
