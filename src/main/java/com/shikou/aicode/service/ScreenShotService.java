package com.shikou.aicode.service;

public interface ScreenShotService {
    String generateAndUploadScreenshot(String url);

    String generateAndUploadScreenshot(Long appId, String url);
}
