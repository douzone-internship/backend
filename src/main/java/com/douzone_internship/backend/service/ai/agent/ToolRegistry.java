package com.douzone_internship.backend.service.ai.agent;

import com.google.genai.types.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, ToolSpec> byName;
    private final List<Tool> tools;

    public ToolRegistry(List<ToolSpec> specs) {
        this.byName = specs.stream()
                .collect(Collectors.toUnmodifiableMap(ToolSpec::name, s -> s));

        Tool tool = Tool.builder()
                .functionDeclarations(specs.stream().map(ToolSpec::declaration).toList())
                .build();
        this.tools = List.of(tool);

        log.info("ToolRegistry initialized with {} tools: {}", specs.size(), byName.keySet());
    }

    public List<Tool> tools() {
        return tools;
    }

    public Object invoke(String name, Map<String, Object> args) {
        ToolSpec spec = byName.get(name);
        if (spec == null) {
            throw new UnknownToolException(name);
        }
        log.debug("Tool invoked: name={}, args={}", name, args);
        return spec.invoke(args);
    }

    public boolean has(String name) {
        return byName.containsKey(name);
    }

    public static class UnknownToolException extends RuntimeException {
        public UnknownToolException(String name) {
            super("Unknown tool: " + name);
        }
    }
}
