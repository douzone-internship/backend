package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.service.ResultService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/result")
@Tag(name = "Result", description = "검색 결과 및 AI 분석 API")
public class ResultController {

    private final ResultService resultService;

    @Operation(summary = "검색 결과 생성", description = "진료코드, 지역, 병원명 조건으로 비급여 진료비를 조회하고 AI 분석 코멘트를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 결과 및 AI 코멘트 반환"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패")
    })
    @PostMapping("/reports")
    public ResponseEntity<ResultListResponseDTO> generateReport(@Validated @RequestBody ResultRequest resultRequest) {
        return ResponseEntity.ok(resultService.generateResult(resultRequest));
    }
}
