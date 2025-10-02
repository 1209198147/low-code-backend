package com.shikou.aicode.ai;

import dev.langchain4j.service.SystemMessage;
import reactor.core.publisher.Flux;

public interface AiGeneratorService {
    @SystemMessage(fromResource = "prompts/html_prompt.txt")
    Flux<String> generateHtmlStream(String userMessage);

    @SystemMessage(fromResource = "prompts/multiple_files_prompt.txt")
    Flux<String> generateMutiFileStream(String userMessage);
}
