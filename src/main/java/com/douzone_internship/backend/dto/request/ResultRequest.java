package com.douzone_internship.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResultRequest(

    @NotBlank(message = "진료코드는 필수입니다")
    String clinicCode,

    String hospitalName,

    @Pattern(regexp = "^\\d{10}$", message = "지역코드는 10자리 숫자여야 합니다")
    String locationCode
) {}
