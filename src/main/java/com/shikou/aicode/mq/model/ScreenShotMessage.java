package com.shikou.aicode.mq.model;

import lombok.Data;

@Data
public class ScreenShotMessage {
    private String url;
    private Long appId;
}
