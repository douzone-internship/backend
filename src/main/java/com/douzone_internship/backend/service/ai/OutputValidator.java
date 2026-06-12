package com.douzone_internship.backend.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OutputValidator {

    private static final int MAX_LINES = 8;
    private static final Pattern STRIKETHROUGH = Pattern.compile("~~+");
    private static final Pattern MARKDOWN_CHARS = Pattern.compile("[*#`_>]+");

    public String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String stripped = STRIKETHROUGH.matcher(raw).replaceAll("");
        stripped = MARKDOWN_CHARS.matcher(stripped).replaceAll("");
        boolean markdownRemoved = !stripped.equals(raw);

        List<String> nonEmpty = stripped.lines()
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();

        boolean truncated = nonEmpty.size() > MAX_LINES;
        List<String> finalLines = truncated ? nonEmpty.subList(0, MAX_LINES) : nonEmpty;

        if (markdownRemoved || truncated) {
            log.info("AI 출력 검증 수정 적용: markdownRemoved={}, truncated={} ({}→{}줄)",
                    markdownRemoved, truncated, nonEmpty.size(), finalLines.size());
        }

        return String.join("\n", finalLines);
    }
}
