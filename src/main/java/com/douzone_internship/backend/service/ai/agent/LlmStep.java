package com.douzone_internship.backend.service.ai.agent;

import java.util.Map;

public sealed interface LlmStep {

    record ToolCall(String name, Map<String, Object> args) implements LlmStep {}

    record FinalText(String text) implements LlmStep {}
}
