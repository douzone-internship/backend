package com.douzone_internship.backend.service.ai.tools;

import java.util.List;

public record NegativeFlagReport(
        String hospitalName,
        String clinicCode,
        int negativeReviewCount,
        int totalReviews,
        List<String> hitKeywords,
        String severity
) {
    public static NegativeFlagReport unknown(String hospitalName, String clinicCode) {
        return new NegativeFlagReport(hospitalName, clinicCode, 0, 0, List.of(), "UNKNOWN");
    }
}
