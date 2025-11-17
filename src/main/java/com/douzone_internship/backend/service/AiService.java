package com.douzone_internship.backend.service;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    @Value("${system-prompt}")
    private String systemPrompt;

    @Value("${user-prompt}")
    private String userPrompt;

    private final Client geminiClient;

    public String callAiApi(List<ResultItemDTO> resultItems) {
        try {

            String itemsData = resultItems.stream()
                    .map(item -> String.format("- %s", item.toString()))
                    .collect(Collectors.joining("\n"));

            String formattedUserPrompt = userPrompt.replace("{{resultItems}}", itemsData);

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
        } catch (Exception e) {
            log.error("AI 호출 중 오류 발생", e);
            return "AI 호출 실패";
        }
    }
}
