package com.douzone_internship.backend.service.ai.cache;

import com.douzone_internship.backend.domain.AiComment;
import com.douzone_internship.backend.domain.SearchLog;
import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.repository.AiCommentRepository;
import com.douzone_internship.backend.repository.CommentRepository;
import com.douzone_internship.backend.repository.ResultRepository;
import com.douzone_internship.backend.repository.SearchLogRepository;
import com.douzone_internship.backend.util.SHA256;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseCache {

    private final SearchLogRepository searchLogRepository;
    private final AiCommentRepository aiCommentRepository;
    private final ResultRepository resultRepository;
    private final CommentRepository commentRepository;
    private final SHA256 sha256;

    public String buildPlainKeyword(ResultRequest req) {
        // 해당 진료코드의 후기 수를 키에 포함 → 후기가 추가되면 키가 바뀌어 캐시가 자동 무효화된다.
        // 새 후기가 달려도 옛 AI 리포트가 반환되던 stale 문제를 방지.
        long commentCount = commentRepository.countByClinicCode(nullSafe(req.clinicCode()));
        return nullSafe(req.clinicCode())
                + nullSafe(req.sidoCode())
                + nullSafe(req.sigguCode())
                + nullSafe(req.hospitalName())
                + "|c=" + commentCount;
    }

    public String buildKey(ResultRequest req) {
        try {
            return sha256.encrypt(buildPlainKeyword(req));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<ResultListResponseDTO> load(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }
        Optional<ResultListResponseDTO> result = searchLogRepository.findBySearchKeyword(key)
                .map(this::buildResponse);
        log.debug("AiResponseCache 조회: key={}, hit={}", key, result.isPresent());
        return result;
    }

    private ResultListResponseDTO buildResponse(SearchLog searchLog) {
        List<ResultItemDTO> items = resultRepository.findBySearchLogWithFetch(searchLog)
                .stream()
                .map(r -> new ResultItemDTO(
                        r.getHospitalName(),
                        r.getHospitalAddress(),
                        r.getClinicName(),
                        r.getMinPrice(),
                        r.getMaxPrice()))
                .toList();
        String aiComment = aiCommentRepository.findBySearchLog(searchLog)
                .map(AiComment::getComment)
                .orElse("");
        return ResultListResponseDTO.builder()
                .resultCount(items.size())
                .list(items)
                .aiComment(aiComment)
                .build();
    }

    private String nullSafe(String s) {
        return s == null ? "null" : s;
    }
}
