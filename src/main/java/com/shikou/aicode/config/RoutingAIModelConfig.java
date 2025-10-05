package com.shikou.aicode.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@ConfigurationProperties("langchain4j.open-ai.routing-chat-model")
@Data
public class RoutingAIModelConfig {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private int maxTokens;
    private  boolean logRequests;
    private boolean logResponses;
    private Double temperature;

    @Bean
    @Scope("prototype")
    public ChatModel routingAIModelPrototype(){
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .temperature(temperature)
                .build();
    }
}
