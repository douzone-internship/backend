package com.douzone_internship.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResultRequest(

    @NotBlank(message = "진료코드는 필수입니다")
    String clinicCode,

    String hospitalName,

    @Pattern(regexp = "^\\d{6}$", message = "시도 코드는 6자리 숫자여야 합니다")
    String sidoCode,

    @Pattern(regexp = "^\\d{6}$", message = "시군구 코드는 6자리 숫자여야 합니다")
    String sigguCode
) {}
