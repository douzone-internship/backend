package com.douzone_internship.backend.eval;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.service.ai.PriceStats;
import com.douzone_internship.backend.service.ai.PriceStatsCalculator;
import com.douzone_internship.backend.service.ai.PromptBuilder;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.CountTokensResponse;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 입력 토큰 절감 측정 — Before(toString join) vs After(PromptBuilder).
 * <p>
 * 실행: ./gradlew test --tests TokenCountBench
 * 결과: 콘솔에 절감률 출력. LLM 호출 없이 토큰 카운트만 받음 (무료).
 */
@SpringBootTest
@ActiveProfiles("local")
@Tag("eval")
class TokenCountBench {

    private static final String MODEL = "gemini-2.5-flash";

    @Autowired
    private Client geminiClient;

    @Autowired
    private PriceStatsCalculator priceStatsCalculator;

    @Autowired
    private PromptBuilder promptBuilder;

    @Test
    void compareTokenUsage() {
        List<ResultItemDTO> sample = sampleItems();

        // Before: 기존 방식 — record toString 직접 join
        String beforePrompt = "[시술 코드] BO001\n\n" + sample.stream()
                .map(item -> "- " + item.toString())
                .collect(Collectors.joining("\n"));

        // After: PromptBuilder 직조
        PriceStats stats = priceStatsCalculator.compute(sample);
        String afterPrompt = promptBuilder.build("{{resultItems}}", sample, stats, "BO001");

        int beforeTokens = countTokens(beforePrompt);
        int afterTokens = countTokens(afterPrompt);
        double savedPct = (beforeTokens - afterTokens) * 100.0 / beforeTokens;

        System.out.println();
        System.out.println("===== TokenCountBench 결과 =====");
        System.out.println("샘플 병원 수: " + sample.size());
        System.out.println("Before (toString join) 토큰: " + beforeTokens);
        System.out.println("After  (PromptBuilder)  토큰: " + afterTokens);
        System.out.printf ("절감률: %.1f%% (%d → %d)%n", savedPct, beforeTokens, afterTokens);
        System.out.println("================================");
        System.out.println();
        System.out.println("---- Before prompt ----");
        System.out.println(beforePrompt);
        System.out.println("---- After prompt ----");
        System.out.println(afterPrompt);
    }

    private int countTokens(String text) {
        Content content = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(text)))
                .build();
        CountTokensResponse response = geminiClient.models.countTokens(MODEL, List.of(content), null);
        return response.totalTokens().orElse(0);
    }

    private List<ResultItemDTO> sampleItems() {
        return List.of(
                new ResultItemDTO("강남필의원", "서울 강남구 테헤란로 123", "보톡스주사", 30000, 50000),
                new ResultItemDTO("강남미인의원", "서울 강남구 역삼동 45-6", "보톡스주사", 35000, 55000),
                new ResultItemDTO("청담리프트의원", "서울 강남구 청담동 88", "보톡스주사", 45000, 70000),
                new ResultItemDTO("논현스타의원", "서울 강남구 논현동 12-3", "보톡스주사", 28000, 48000),
                new ResultItemDTO("역삼우주의원", "서울 강남구 역삼동 200", "보톡스주사", 40000, 60000)
        );
    }
}
