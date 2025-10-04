package com.shikou.aicode;

import com.shikou.aicode.ai.AiCodeGenTypeRoutingService;
import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TestRoute {
    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    public void testRoute(){
        String initPrompt = "创建一个单页面的，代码不超过200行的个人简介网站";
        CodeGenTypeEnum typeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        System.out.println(typeEnum.getText());
    }
}
