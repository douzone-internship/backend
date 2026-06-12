package com.douzone_internship.backend.service.ai.tools;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NegativeFlagTool {

    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "사고", "불만", "재수술", "부작용", "통증",
            "실수", "잘못", "감염", "후회", "최악",
            "실패", "환불"
    );

    private static final int WARNING_ABS = 3;
    private static final double WARNING_RATIO = 0.40;
    private static final int CAUTION_ABS = 1;
    private static final double CAUTION_RATIO = 0.15;

    private static final int TOP_KEYWORDS = 3;

    private final CommentRepository commentRepository;

    public NegativeFlagReport getNegativeFlags(String hospitalName, String clinicCode) {
        if (isBlank(hospitalName) || isBlank(clinicCode)) {
            return NegativeFlagReport.unknown(hospitalName, clinicCode);
        }

        List<Comment> all = commentRepository
                .findByHospitalNameAndClinicCodeOrderByCreatedAtDesc(hospitalName, clinicCode);

        if (all.isEmpty()) {
            return NegativeFlagReport.unknown(hospitalName, clinicCode);
        }

        Map<String, Integer> keywordHits = new LinkedHashMap<>();
        int negativeReviewCount = 0;

        for (Comment c : all) {
            String body = c.getComment();
            if (body == null || body.isEmpty()) continue;

            boolean hit = false;
            for (String kw : NEGATIVE_KEYWORDS) {
                if (body.contains(kw)) {
                    keywordHits.merge(kw, 1, Integer::sum);
                    hit = true;
                }
            }
            if (hit) negativeReviewCount++;
        }

        List<String> topKeywords = keywordHits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_KEYWORDS)
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .toList();

        String severity = classify(negativeReviewCount, all.size());

        return new NegativeFlagReport(
                hospitalName, clinicCode,
                negativeReviewCount, all.size(),
                topKeywords, severity
        );
    }

    private String classify(int negativeCount, int total) {
        if (total == 0) return "UNKNOWN";
        double ratio = (double) negativeCount / total;
        if (negativeCount >= WARNING_ABS || ratio >= WARNING_RATIO) return "WARNING";
        if (negativeCount >= CAUTION_ABS || ratio >= CAUTION_RATIO) return "CAUTION";
        return "SAFE";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
