package com.shikou.aicode.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConfigurationProperties("langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamChatModelConfig {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private  boolean logRequests;
    private boolean logResponses;
    private Double temperature;

    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype(){
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logResponses(logResponses)
                .logRequests(logRequests)
                .temperature(temperature)
                .build();
    }
}
