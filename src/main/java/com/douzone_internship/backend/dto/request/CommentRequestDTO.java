package com.douzone_internship.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.springframework.validation.annotation.Validated;

@Builder
@Validated
public record CommentRequestDTO(
        @NotBlank String hospitalName,
        @NotBlank String clinicCode,
        @NotBlank String comment,
        @Min(1) @Max(5) int score
){}
