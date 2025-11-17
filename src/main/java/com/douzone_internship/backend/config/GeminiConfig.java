package com.douzone_internship.backend.config;
import com.google.genai.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GeminiConfig {

    @Value("${api-key}")
    private String apiKey;

    @Bean
    public Client geminiClient(GeminiProperties properties) {
        return Client.builder()
                .apiKey(apiKey)
                .build();
    }
}