package com.shikou.aicode;

import com.shikou.aicode.service.ScreenShotService;
import com.shikou.aicode.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestWebScreenShot {

    @Resource
    private ScreenShotService screenShotService;

    @Test
    public void testScreenShot(){
        WebScreenshotUtils.saveWebScreenShot("https://www.baidu.com");
    }

    @Test
    public void testScreenShotService(){
        String url = "https://www.bilibili.com/";
        String cosUrl = screenShotService.generateAndUploadScreenshot(url);
        System.out.println(cosUrl);
        Assertions.assertNotEquals(cosUrl, null);
    }
}
