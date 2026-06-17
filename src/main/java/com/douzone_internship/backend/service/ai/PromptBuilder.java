package com.douzone_internship.backend.service.ai;

import com.douzone_internship.backend.dto.response.ResultItemDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class PromptBuilder {

    private static final String PLACEHOLDER = "{{resultItems}}";

    /** LLM 프롬프트에 넣을 병원 수 상한. 초과 시 가격 분포의 양 끝(저가/고가)만 추린다. */
    private static final int MAX_ITEMS = 20;
    private static final int HALF = MAX_ITEMS / 2;

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

        // 통계는 전체로 이미 계산됨. LLM 추천 후보 목록만 토큰 절감을 위해 제한한다.
        // 병원 수가 MAX_ITEMS 초과 시 가격 분포의 양 끝(저가 HALF + 고가 HALF)만 추려
        // 입력 규모와 무관하게 토큰을 일정하게 유지한다.
        List<ResultItemDTO> selected = selectForPrompt(items);

        String body = selected.stream()
                .map(this::formatItem)
                .collect(Collectors.joining("\n"));

        String header = selected.size() < items.size()
                ? String.format("[병원 목록] (총 %d곳 중 가격 저가 %d·고가 %d곳 발췌)", items.size(), HALF, HALF)
                : "[병원 목록]";
        return header + "\n" + body;
    }

    private List<ResultItemDTO> selectForPrompt(List<ResultItemDTO> items) {
        if (items.size() <= MAX_ITEMS) {
            return items;
        }
        List<ResultItemDTO> sorted = items.stream()
                .sorted(Comparator.comparingInt(ResultItemDTO::minPrice))
                .toList();
        Stream<ResultItemDTO> low = sorted.subList(0, HALF).stream();
        Stream<ResultItemDTO> high = sorted.subList(sorted.size() - HALF, sorted.size()).stream();
        return Stream.concat(low, high).distinct().toList();
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
