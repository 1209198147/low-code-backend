package com.shikou.aicode.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface AiGeneratorService {
    @SystemMessage(fromResource = "prompts/html_prompt.txt")
    Flux<String> generateHtmlStream(String userMessage);

    @SystemMessage(fromResource = "prompts/multiple_files_prompt.txt")
    Flux<String> generateMutiFileStream(String userMessage);

    @SystemMessage(fromResource = "prompts/vue_project_prompt.txt")
    TokenStream generateVueProjectStream(@MemoryId long appId, @UserMessage String userMessage);
}
