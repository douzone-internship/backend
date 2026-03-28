package com.douzone_internship.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CommentRequestDTO(
        @NotBlank(message = "병원명은 필수입니다.") String hospitalName,
        @NotBlank(message = "진료코드는 필수입니다.") String clinicCode,
        @NotBlank(message = "댓글 내용은 필수입니다.") @Size(max = 500, message = "댓글은 500자 이내여야 합니다.") String comment,
        @Min(value = 1, message = "평점은 1 이상이어야 합니다.") @Max(value = 5, message = "평점은 5 이하여야 합니다.") int score
){}
