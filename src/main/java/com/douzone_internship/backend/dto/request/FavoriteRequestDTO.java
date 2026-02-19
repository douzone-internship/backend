package com.douzone_internship.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FavoriteRequestDTO {
    @NotBlank
    private String hospitalName;

    @NotBlank
    private String clinicName;

    @NotBlank
    private String clinicCode;

    private String location;

    private Integer minPrice;
    private Integer maxPrice;
}
