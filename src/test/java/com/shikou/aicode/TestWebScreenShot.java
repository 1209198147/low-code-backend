package com.shikou.aicode;

import com.shikou.aicode.utils.WebScreenshotUtils;
import org.junit.jupiter.api.Test;

public class TestWebScreenShot {
    @Test
    public void testScreenShot(){
        WebScreenshotUtils.saveWebScreenShot("https://www.baidu.com");
    }
}
