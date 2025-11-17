package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.AiComment;
import com.douzone_internship.backend.domain.Clinic;
import com.douzone_internship.backend.domain.Result;
import com.douzone_internship.backend.domain.SearchLog;
import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.repository.AiCommentRepository;
import com.douzone_internship.backend.repository.ClinicRepository;
import com.douzone_internship.backend.repository.ResultRepository;
import com.douzone_internship.backend.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResultSaveService {

    private final SearchLogRepository searchLogRepository;
    private final AiCommentRepository aiCommentRepository;
    private final ClinicRepository clinicRepository;
    private final ResultRepository resultRepository;

    @Async
    @Transactional
    public void saveResultAsync(ResultRequest resultRequest, List<ResultItemDTO> resultItems, String aiComment) {

        SearchLog searchLog = SearchLog.builder()
                .searchKeyword(resultRequest.sidoCode() + resultRequest.sigguCode() + resultRequest.hospitalName())
                .createdAt(LocalDateTime.now())
                .build();

        SearchLog savedSearchLog = searchLogRepository.save(searchLog);

        AiComment savedAiComment = AiComment.builder()
                .searchLog(savedSearchLog)
                .comment(aiComment)
                .build();

        aiCommentRepository.save(savedAiComment);
        Clinic clinic = clinicRepository.getReferenceById(resultRequest.clinicCode());

        List<Result> results = resultItems.stream()
                .map(item -> {

                    return Result.builder()
                            .searchLog(savedSearchLog)
                            .clinicName(clinic.getName())
                            .hospitalName(item.hospitalName())
                            .minPrice(item.minPrice())
                            .maxPrice(item.maxPrice())
                            .hospitalAddress(item.location())
                            .build();
                })
                .collect(Collectors.toList());

        resultRepository.saveAll(results);
    }
}

