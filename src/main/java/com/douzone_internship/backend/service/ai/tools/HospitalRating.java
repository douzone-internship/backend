package com.douzone_internship.backend.service.ai.tools;

public record HospitalRating(
        String hospitalName,
        String clinicCode,
        double averageScore,
        int count
) {
    public static HospitalRating empty(String hospitalName, String clinicCode) {
        return new HospitalRating(hospitalName, clinicCode, 0.0, 0);
    }

    public boolean isEmpty() {
        return count == 0;
    }
}
