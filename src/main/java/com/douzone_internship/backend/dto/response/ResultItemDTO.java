package com.douzone_internship.backend.dto.response;

import lombok.Builder;

@Builder
public record ResultItemDTO(
    String hospitalName,
    String location,
    String clinicName,
    Integer price
) {}

