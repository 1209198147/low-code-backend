package com.shikou.aicode;

import com.shikou.aicode.config.task.CleanGuestDataTaskConfig;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestCleanGuestData {
    @Resource
    private CleanGuestDataTaskConfig cleanGuestDataTaskConfig;

    @Test
    void testCleanGuestDataTaskConfig(){
        cleanGuestDataTaskConfig.cleanUpGuestData();
    }
}
