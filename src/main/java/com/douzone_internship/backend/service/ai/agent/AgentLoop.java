package com.douzone_internship.backend.service.ai.agent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentLoop {

    static final int MAX_STEPS = 5;
    static final int TOKEN_BUDGET = 8000;

    private final ToolRegistry toolRegistry;

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
        // 4-6에서 Gemini Content로 변환하여 ctx.conversation 에 추가
    }

    private LlmStep callLlm(AgentContext ctx) {
        // 4-6에서 Gemini 호출 → functionCall 또는 text 응답 → LlmStep으로 매핑
        throw new UnsupportedOperationException("Gemini 통합은 4-6에서 구현");
    }

    private void appendToolResult(AgentContext ctx, String toolName, Object result) {
        // 4-6에서 FunctionResponse Content로 변환하여 ctx.conversation 에 추가
    }
}
