package com.shikou.aicode.ai;

import com.shikou.aicode.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

public interface AiCodeGenTypeRoutingService {
    @SystemMessage(fromResource = "prompts/codegen-routing-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userMessage);

    @SystemMessage(fromResource = "prompts/codegen-title-prompt.txt")
    String generateAppName(String userMessage);
}
