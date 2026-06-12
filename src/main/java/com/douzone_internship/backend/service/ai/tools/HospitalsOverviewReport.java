package com.douzone_internship.backend.service.ai.tools;

import java.util.List;

public record HospitalsOverviewReport(
        String clinicCode,
        List<HospitalOverview> hospitals
) {
    public static HospitalsOverviewReport empty(String clinicCode) {
        return new HospitalsOverviewReport(clinicCode, List.of());
    }
}
