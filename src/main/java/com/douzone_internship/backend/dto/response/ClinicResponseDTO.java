package com.douzone_internship.backend.dto.response;

import lombok.Builder;

@Builder
public record ClinicResponseDTO(
    String clinicName,
    String clinicCode
) {}
