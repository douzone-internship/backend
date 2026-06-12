package com.douzone_internship.backend.service.ai.tools;

import java.util.List;

public record HospitalOverview(
        String hospitalName,
        int reviewCount,
        double averageRating,
        String severity,
        List<String> negativeKeywords,
        List<ReviewSnippet> topReviews
) {
    public static HospitalOverview unknown(String hospitalName) {
        return new HospitalOverview(hospitalName, 0, 0.0, "UNKNOWN", List.of(), List.of());
    }
}
