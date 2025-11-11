package com.douzone_internship.backend.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record ClinicListResponseDTO(
        List<ClinicResponseDTO> clinicResponseDTOList
) {}
