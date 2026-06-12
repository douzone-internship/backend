package com.douzone_internship.backend.service.ai.tools;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HospitalsOverviewTool {

    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "재수술", "부작용", "감염", "후회",
            "최악", "환불", "사기",
            "고소", "분쟁", "민원"
    );

    private static final int WARNING_ABS = 3;
    private static final double WARNING_RATIO = 0.40;
    private static final int CAUTION_ABS = 1;
    private static final double CAUTION_RATIO = 0.15;

    private static final int TOP_KEYWORDS = 3;
    private static final int TOP_SNIPPETS = 2;
    private static final int SNIPPET_MAX_CHARS = 150;

    private final CommentRepository commentRepository;

    public HospitalsOverviewReport getOverview(List<String> hospitalNames, String clinicCode) {
        if (hospitalNames == null || hospitalNames.isEmpty() || isBlank(clinicCode)) {
            return HospitalsOverviewReport.empty(clinicCode);
        }

        List<String> distinctNames = hospitalNames.stream().distinct().toList();

        List<Comment> all = commentRepository
                .findByHospitalNameInAndClinicCodeOrderByCreatedAtDesc(distinctNames, clinicCode);

        Map<String, List<Comment>> byHospital = all.stream()
                .collect(Collectors.groupingBy(Comment::getHospitalName));

        List<HospitalOverview> overviews = distinctNames.stream()
                .map(name -> buildOverview(name, byHospital.getOrDefault(name, List.of())))
                .toList();

        return new HospitalsOverviewReport(clinicCode, overviews);
    }

    private HospitalOverview buildOverview(String hospitalName, List<Comment> comments) {
        if (comments.isEmpty()) {
            return HospitalOverview.unknown(hospitalName);
        }

        int count = comments.size();

        double rawAvg = comments.stream().mapToInt(Comment::getScore).average().orElse(0.0);
        double averageRating = Math.round(rawAvg * 10.0) / 10.0;

        Map<String, Integer> keywordHits = new LinkedHashMap<>();
        int negativeCount = 0;
        for (Comment c : comments) {
            String body = c.getComment();
            if (body == null || body.isEmpty()) continue;
            boolean hit = false;
            for (String kw : NEGATIVE_KEYWORDS) {
                if (body.contains(kw)) {
                    keywordHits.merge(kw, 1, Integer::sum);
                    hit = true;
                }
            }
            if (hit) negativeCount++;
        }

        List<String> topKeywords = keywordHits.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_KEYWORDS)
                .map(e -> e.getKey() + "(" + e.getValue() + ")")
                .toList();

        String severity = classify(negativeCount, count);

        List<ReviewSnippet> topReviews = comments.stream()
                .limit(TOP_SNIPPETS)
                .map(this::toSnippet)
                .toList();

        return new HospitalOverview(hospitalName, count, averageRating, severity, topKeywords, topReviews);
    }

    private ReviewSnippet toSnippet(Comment c) {
        String body = c.getComment() == null ? "" : c.getComment();
        String truncated = body.length() <= SNIPPET_MAX_CHARS
                ? body
                : body.substring(0, SNIPPET_MAX_CHARS) + "...";
        LocalDateTime created = c.getCreatedAt();
        return new ReviewSnippet(truncated, c.getScore(), created == null ? "" : created.toString());
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
