package com.douzone_internship.backend.dto.response;

import lombok.Builder;
import java.util.List;

@Builder
public record ResultListResponseDTO(
    Integer resultCount,
    String aiComment,
    List<ResultItemDTO> list
) {
    public static ResultListResponseDTO of(Integer resultCount, String aiComment, List<ResultItemDTO> list) {
        return ResultListResponseDTO.builder()
            .resultCount(resultCount)
            .aiComment(aiComment)
            .list(list)
            .build();
    }
}
