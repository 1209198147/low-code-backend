package com.shikou.aicode.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("langchain4j.openai.chat-model")
@Data
public class ReasoningStreamChatModelConfig {
    private String baseUrl;
    private String apiKey;

    @Bean
    public StreamingChatModel reasoningStreamingChatModel(){
        String modelName = "deepseek-chat";
        int maxToken = 8192;

//        modelName = "deepseek-reasoner";
//        maxToken = 32768;

        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxToken)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
