package com.douzone_internship.backend.dto.response;

import com.douzone_internship.backend.domain.Comment;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record CommentResponseDTO(
        UUID id,
        String userName,
        String hospitalName,
        String clinicCode,
        String comment,
        int score,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    // 병원별 댓글 조회용 (hospitalName/clinicCode 미포함, 익명 처리)
    public static CommentResponseDTO from(Comment c, boolean isOwner) {
        return CommentResponseDTO.builder()
                .id(c.getId())
                .userName(isOwner ? c.getUser().getName() : null)
                .comment(c.getComment())
                .score(c.getScore())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    // 내 댓글 목록용 (hospitalName/clinicCode 포함)
    public static CommentResponseDTO fromMy(Comment c) {
        return CommentResponseDTO.builder()
                .id(c.getId())
                .userName(c.getUser().getName())
                .hospitalName(c.getHospitalName())
                .clinicCode(c.getClinicCode())
                .comment(c.getComment())
                .score(c.getScore())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
