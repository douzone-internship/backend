package com.douzone_internship.backend.service.ai.agent;

import com.google.genai.types.Content;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class AgentContext {

    private final List<Content> conversation = new ArrayList<>();
    private final List<String> toolCallSignatures = new ArrayList<>();

    private int stepCount = 0;
    private int promptTokensUsed = 0;
    private int responseTokensUsed = 0;

    public void incrementStep() {
        stepCount++;
    }

    public int totalTokens() {
        return promptTokensUsed + responseTokensUsed;
    }

    public void recordTokens(int promptDelta, int responseDelta) {
        this.promptTokensUsed += promptDelta;
        this.responseTokensUsed += responseDelta;
    }

    public void addToConversation(Content c) {
        conversation.add(c);
    }

    public boolean isRepeatOfPrevious(String toolName, Map<String, Object> args) {
        String signature = toolName + "|" + args;
        boolean repeat = !toolCallSignatures.isEmpty()
                && toolCallSignatures.get(toolCallSignatures.size() - 1).equals(signature);
        toolCallSignatures.add(signature);
        return repeat;
    }
}
