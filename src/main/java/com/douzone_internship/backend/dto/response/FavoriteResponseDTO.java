package com.douzone_internship.backend.dto.response;

import com.douzone_internship.backend.domain.Favorite;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class FavoriteResponseDTO {
    private UUID id;
    private String hospitalName;
    private String clinicName;
    private String clinicCode;
    private String location;
    private Integer minPrice;
    private Integer maxPrice;
    private LocalDateTime createdAt;

    public static FavoriteResponseDTO from(Favorite favorite) {
        return FavoriteResponseDTO.builder()
                .id(favorite.getId())
                .hospitalName(favorite.getHospitalName())
                .clinicName(favorite.getClinicName())
                .clinicCode(favorite.getClinicCode())
                .location(favorite.getLocation())
                .minPrice(favorite.getMinPrice())
                .maxPrice(favorite.getMaxPrice())
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
