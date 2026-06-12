package com.douzone_internship.backend.service;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.service.ai.PriceStats;
import com.douzone_internship.backend.service.ai.PriceStatsCalculator;
import com.douzone_internship.backend.service.ai.PromptBuilder;
import com.google.genai.Client;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    @Value("${system-prompt}")
    private String systemPrompt;

    @Value("${user-prompt}")
    private String userPrompt;

    private final Client geminiClient;
    private final PriceStatsCalculator priceStatsCalculator;
    private final PromptBuilder promptBuilder;

    @Retry(name = "geminiApi")
    @CircuitBreaker(name = "geminiApi", fallbackMethod = "aiApiFallback")
    public String callAiApi(List<ResultItemDTO> resultItems) {
        PriceStats stats = priceStatsCalculator.compute(resultItems);
        String formattedUserPrompt = promptBuilder.build(userPrompt, resultItems, stats);

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                        .build();

        GenerateContentResponse response = geminiClient.models.generateContent(
                "gemini-2.5-flash",
                formattedUserPrompt,
                config
        );
        return response.text();
    }

    private String aiApiFallback(List<ResultItemDTO> resultItems, Throwable t) {
        log.error("AI API 호출 실패, fallback 실행", t);
        return "AI 분석을 일시적으로 제공할 수 없습니다.";
    }
}
