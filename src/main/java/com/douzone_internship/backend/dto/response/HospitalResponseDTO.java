package com.douzone_internship.backend.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record HospitalResponseDTO(
    List<String> nameList
) {}
