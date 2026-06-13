package com.douzone_internship.backend.service;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.service.ai.OutputValidator;
import com.douzone_internship.backend.service.ai.PriceStats;
import com.douzone_internship.backend.service.ai.PriceStatsCalculator;
import com.douzone_internship.backend.service.ai.PromptBuilder;
import com.douzone_internship.backend.service.ai.agent.AgentLoop;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    @Value("${user-prompt}")
    private String userPrompt;

    private final PriceStatsCalculator priceStatsCalculator;
    private final PromptBuilder promptBuilder;
    private final OutputValidator outputValidator;
    private final AgentLoop agentLoop;

    @Retry(name = "geminiApi")
    @CircuitBreaker(name = "geminiApi", fallbackMethod = "aiApiFallback")
    public String callAiApi(List<ResultItemDTO> resultItems, String clinicCode) {
        PriceStats stats = priceStatsCalculator.compute(resultItems);
        String formattedUserPrompt = promptBuilder.build(userPrompt, resultItems, stats, clinicCode);

        String agentResponse = agentLoop.run(formattedUserPrompt);
        return outputValidator.validate(agentResponse);
    }

    private String aiApiFallback(List<ResultItemDTO> resultItems, String clinicCode, Throwable t) {
        log.error("AI API 호출 실패, fallback 실행", t);
        return "AI 분석을 일시적으로 제공할 수 없습니다.";
    }
}
