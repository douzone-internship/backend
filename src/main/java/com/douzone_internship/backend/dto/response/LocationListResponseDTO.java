package com.douzone_internship.backend.dto.response;

import java.util.List;

public record LocationListResponseDTO(
    List<LocationResponseDTO> locations
) {
    public static LocationListResponseDTO of(List<LocationResponseDTO> locations) {
        return new LocationListResponseDTO(locations);
    }
}

