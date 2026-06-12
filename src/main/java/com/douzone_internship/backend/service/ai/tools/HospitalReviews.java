package com.douzone_internship.backend.service.ai.tools;

import java.util.List;

public record HospitalReviews(
        String hospitalName,
        String clinicCode,
        int totalCount,
        List<ReviewSnippet> samples
) {
    public static HospitalReviews empty(String hospitalName, String clinicCode) {
        return new HospitalReviews(hospitalName, clinicCode, 0, List.of());
    }

    public boolean isEmpty() {
        return totalCount == 0;
    }
}
