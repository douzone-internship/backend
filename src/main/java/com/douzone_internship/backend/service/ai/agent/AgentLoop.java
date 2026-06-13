package com.douzone_internship.backend.service.ai.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AgentLoop {

    static final int MAX_STEPS = 5;
    static final int TOKEN_BUDGET = 8000;
    private static final String MODEL = "gemini-2.5-flash";

    private final ToolRegistry toolRegistry;
    private final Client geminiClient;
    private final ObjectMapper objectMapper;
    private final String systemPrompt;

    public AgentLoop(
            ToolRegistry toolRegistry,
            Client geminiClient,
            ObjectMapper objectMapper,
            @Value("${system-prompt}") String systemPrompt
    ) {
        this.toolRegistry = toolRegistry;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.systemPrompt = systemPrompt;
    }

    public String run(String userPrompt) {
        AgentContext ctx = new AgentContext();
        seedUserMessage(ctx, userPrompt);

        while (true) {
            ctx.incrementStep();

            if (ctx.getStepCount() > MAX_STEPS) {
                log.warn("AgentLoop 종료: MAX_STEPS({}) 초과", MAX_STEPS);
                return "최대 단계를 초과해 응답을 완성하지 못했습니다.";
            }
            if (ctx.totalTokens() > TOKEN_BUDGET) {
                log.warn("AgentLoop 종료: 토큰 예산({}) 초과, 누적={}", TOKEN_BUDGET, ctx.totalTokens());
                return "토큰 예산을 초과해 응답을 완성하지 못했습니다.";
            }

            LlmStep step = callLlm(ctx);

            if (step instanceof LlmStep.FinalText finalText) {
                log.info("AgentLoop 종료: 최종 응답 도달 (steps={}, tokens={})",
                        ctx.getStepCount(), ctx.totalTokens());
                return finalText.text();
            }

            if (step instanceof LlmStep.ToolCall toolCall) {
                if (ctx.isRepeatOfPrevious(toolCall.name(), toolCall.args())) {
                    log.warn("AgentLoop 종료: 직전과 동일한 도구 호출 감지 ({})", toolCall.name());
                    return "반복된 도구 호출이 감지되어 응답을 완성하지 못했습니다.";
                }

                Object result = toolRegistry.invoke(toolCall.name(), toolCall.args());
                appendToolResult(ctx, toolCall.name(), result);
            }
        }
    }

    private void seedUserMessage(AgentContext ctx, String userPrompt) {
        Content userMsg = Content.builder()
                .role("user")
                .parts(List.of(Part.fromText(userPrompt)))
                .build();
        ctx.addToConversation(userMsg);
    }

    private LlmStep callLlm(AgentContext ctx) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .tools(toolRegistry.tools())
                .build();

        GenerateContentResponse response = geminiClient.models.generateContent(
                MODEL, ctx.getConversation(), config);

        response.usageMetadata().ifPresent(meta -> {
            int prompt = meta.promptTokenCount().orElse(0);
            int candidates = meta.candidatesTokenCount().orElse(0);
            ctx.recordTokens(prompt, candidates);
        });

        List<Candidate> candidates = response.candidates().orElse(List.of());
        if (candidates.isEmpty()) {
            log.warn("Gemini 응답에 candidates가 비어있음");
            return new LlmStep.FinalText("");
        }

        Content modelContent = candidates.get(0).content().orElse(null);
        if (modelContent == null) {
            log.warn("Gemini candidate에 content가 없음");
            return new LlmStep.FinalText("");
        }
        ctx.addToConversation(modelContent);

        List<Part> parts = modelContent.parts().orElse(List.of());
        for (Part part : parts) {
            if (part.functionCall().isPresent()) {
                FunctionCall call = part.functionCall().get();
                String name = call.name().orElse("");
                Map<String, Object> args = call.args().orElse(Map.of());
                if (name.isBlank()) {
                    log.warn("FunctionCall의 name이 비어있음");
                    return new LlmStep.FinalText(safeText(response));
                }
                return new LlmStep.ToolCall(name, args);
            }
        }

        return new LlmStep.FinalText(safeText(response));
    }

    @SuppressWarnings("unchecked")
    private void appendToolResult(AgentContext ctx, String toolName, Object result) {
        Map<String, Object> responseMap = objectMapper.convertValue(result, Map.class);
        Content functionResponse = Content.builder()
                .role("user")
                .parts(List.of(Part.fromFunctionResponse(toolName, responseMap)))
                .build();
        ctx.addToConversation(functionResponse);
    }

    private String safeText(GenerateContentResponse response) {
        try {
            String t = response.text();
            return t == null ? "" : t;
        } catch (Exception e) {
            log.warn("response.text() 추출 실패: {}", e.getMessage());
            return "";
        }
    }
}
