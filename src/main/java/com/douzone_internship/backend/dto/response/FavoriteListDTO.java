package com.douzone_internship.backend.dto.response;

import com.douzone_internship.backend.domain.Favorite;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record FavoriteListDTO(
        UUID id,
        String hospitalName,
        String location,
        String clinicName,
        String clinicCode,
        Integer minPrice,
        Integer maxPrice,
        LocalDateTime createdAt
) {
    public static FavoriteListDTO from(Favorite favorite) {
        return FavoriteListDTO.builder()
                .id(favorite.getId())
                .hospitalName(favorite.getHospitalName())
                .location(favorite.getLocation())
                .clinicName(favorite.getClinicName())
                .clinicCode(favorite.getClinicCode())
                .minPrice(favorite.getMinPrice())
                .maxPrice(favorite.getMaxPrice())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
