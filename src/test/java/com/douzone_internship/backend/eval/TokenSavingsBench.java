package com.douzone_internship.backend.eval;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.service.ai.PriceStats;
import com.douzone_internship.backend.service.ai.PriceStatsCalculator;
import com.douzone_internship.backend.service.ai.PromptBuilder;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 입력 + 출력 토큰 종합 측정.
 * Mode A: 옛 방식 — toString join, 통계 미계산 (LLM이 직접 계산해야 함)
 * Mode B: 새 방식 — PromptBuilder + 통계 사전 계산 (LLM은 인용만)
 * N회 반복 평균으로 LLM 비결정성 흡수.
 */
@SpringBootTest
@ActiveProfiles("local")
@Tag("eval")
class TokenSavingsBench {

    private static final String MODEL = "gemini-2.5-flash";
    private static final int REPEAT = 3;

    private static final String OLD_SYSTEM_PROMPT =
            "너는 비급여 진료 가격 분석가야. 사용자가 제공한 병원 데이터를 보고 " +
            "최저가, 최고가, 평균가를 직접 계산하고, 추천 병원 3~5개를 선정해줘. " +
            "총 8줄 이하로 작성하고 마크다운 기호는 쓰지 마.";

    @Value("${system-prompt}")
    private String currentSystemPrompt;

    @Autowired private Client geminiClient;
    @Autowired private PriceStatsCalculator priceStatsCalculator;
    @Autowired private PromptBuilder promptBuilder;

    @Test
    void compareFullTokenUsage() {
        List<ResultItemDTO> sample = sampleItems();

        // Mode A: 옛 방식 — toString join, 통계 없음
        String userPromptA = "[시술 코드] BO001\n\n[병원 목록]\n" + sample.stream()
                .map(item -> "- " + item.toString())
                .collect(Collectors.joining("\n"));

        // Mode B: 새 방식 — PromptBuilder + 통계 사전 계산
        PriceStats stats = priceStatsCalculator.compute(sample);
        String userPromptB = promptBuilder.build("{{resultItems}}", sample, stats, "BO001");

        long inputAtotal = 0, outputAtotal = 0;
        long inputBtotal = 0, outputBtotal = 0;

        for (int i = 0; i < REPEAT; i++) {
            Usage uA = callAndMeasure(OLD_SYSTEM_PROMPT, userPromptA);
            Usage uB = callAndMeasure(currentSystemPrompt, userPromptB);
            inputAtotal += uA.input;
            outputAtotal += uA.output;
            inputBtotal += uB.input;
            outputBtotal += uB.output;
            System.out.printf("  [run %d] A: in=%d out=%d, B: in=%d out=%d%n",
                    i + 1, uA.input, uA.output, uB.input, uB.output);
        }

        long inputA = inputAtotal / REPEAT;
        long outputA = outputAtotal / REPEAT;
        long inputB = inputBtotal / REPEAT;
        long outputB = outputBtotal / REPEAT;
        long totalA = inputA + outputA;
        long totalB = inputB + outputB;

        double inputSaved = (inputA - inputB) * 100.0 / inputA;
        double outputSaved = (outputA - outputB) * 100.0 / outputA;
        double totalSaved = (totalA - totalB) * 100.0 / totalA;

        System.out.println();
        System.out.println("===== TokenSavingsBench 결과 (N=" + REPEAT + " 평균) =====");
        System.out.printf("Mode A (옛 — toString + LLM이 통계 계산):%n");
        System.out.printf("  입력: %d, 출력: %d, 총합: %d%n", inputA, outputA, totalA);
        System.out.printf("Mode B (새 — PromptBuilder + 통계 사전계산):%n");
        System.out.printf("  입력: %d, 출력: %d, 총합: %d%n", inputB, outputB, totalB);
        System.out.println();
        System.out.printf("절감률: 입력 %.1f%%, 출력 %.1f%%, 총합 %.1f%%%n",
                inputSaved, outputSaved, totalSaved);
        System.out.println("==================================================");
    }

    private Usage callAndMeasure(String systemPrompt, String userPrompt) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .build();
        GenerateContentResponse response = geminiClient.models.generateContent(MODEL, userPrompt, config);
        int input = response.usageMetadata()
                .flatMap(meta -> meta.promptTokenCount())
                .orElse(0);
        int output = response.usageMetadata()
                .flatMap(meta -> meta.candidatesTokenCount())
                .orElse(0);
        return new Usage(input, output);
    }

    private record Usage(int input, int output) {}

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
