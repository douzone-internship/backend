package com.douzone_internship.backend.dto.response;

import lombok.Builder;

@Builder
public record LocationResponseDTO(
    String locationName,
    String sidoCode,
    String sgguCode
) {}
