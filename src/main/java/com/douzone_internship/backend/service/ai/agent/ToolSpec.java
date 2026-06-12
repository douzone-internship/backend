package com.douzone_internship.backend.service.ai.agent;

import com.google.genai.types.FunctionDeclaration;

import java.util.Map;

public interface ToolSpec {

    String name();

    FunctionDeclaration declaration();

    Object invoke(Map<String, Object> args);
}
