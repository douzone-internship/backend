package com.douzone_internship.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CommentUpdateDTO(
        @NotBlank String comment,
        @Min(1) @Max(5) int score) {
}
