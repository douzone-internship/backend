package com.douzone_internship.backend.service.ai;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PromptBuilder {

    private static final String PLACEHOLDER = "{{resultItems}}";

    public String build(String template, List<ResultItemDTO> items, PriceStats stats, String clinicCode) {
        if (template == null || !template.contains(PLACEHOLDER)) {
            log.warn("user-prompt 템플릿에 {} 자리표시자가 없습니다. 통계/목록이 주입되지 않습니다.", PLACEHOLDER);
        }
        String block = formatClinicCode(clinicCode) + "\n\n"
                + formatStats(stats) + "\n\n"
                + formatItems(items);
        return template == null ? block : template.replace(PLACEHOLDER, block);
    }

    private String formatClinicCode(String code) {
        String value = code == null || code.isBlank() ? "미지정" : code;
        return "[시술 코드] " + value;
    }

    private String formatStats(PriceStats s) {
        if (s.isEmpty()) {
            return "[가격 통계] 유효한 가격 데이터 없음";
        }
        return String.format(
                "[가격 통계] 최저가 %s원 · 최고가 %s원 · 평균 %s원 · 중앙값 %s원 (표본 %d)",
                won(s.min()), won(s.max()), won(s.avg()), won(s.median()), s.sampleSize());
    }

    private String formatItems(List<ResultItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return "[병원 목록] 없음";
        }
        String body = items.stream()
                .map(this::formatItem)
                .collect(Collectors.joining("\n"));
        return "[병원 목록]\n" + body;
    }

    private String formatItem(ResultItemDTO it) {
        String price = (it.minPrice() > 0 && it.maxPrice() > 0)
                ? String.format("%s~%s원", won(it.minPrice()), won(it.maxPrice()))
                : "가격 미공개";
        return String.format("- %s | %s | %s | %s",
                nullSafe(it.hospitalName()),
                nullSafe(it.location()),
                nullSafe(it.clinicName()),
                price);
    }

    private String won(int v) {
        return String.format("%,d", v);
    }

    private String nullSafe(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
