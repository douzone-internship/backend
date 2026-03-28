package com.douzone_internship.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FavoriteRequestDTO {
    @NotBlank(message = "병원명은 필수입니다.")
    private String hospitalName;

    @NotBlank(message = "진료명은 필수입니다.")
    private String clinicName;

    @NotBlank(message = "진료코드는 필수입니다.")
    private String clinicCode;

    private String location;

    @Min(value = 0, message = "최소 가격은 0 이상이어야 합니다.")
    private Integer minPrice;

    @Min(value = 0, message = "최대 가격은 0 이상이어야 합니다.")
    private Integer maxPrice;
}
