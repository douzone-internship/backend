package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.response.ClinicListResponseDTO;
import com.douzone_internship.backend.dto.response.HospitalResponseDTO;
import com.douzone_internship.backend.dto.response.LocationListResponseDTO;
import com.douzone_internship.backend.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/home")
@Validated
@Tag(name = "Home", description = "홈 검색 관련 API (진료코드, 지역, 병원명 조회)")
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "진료명/진료코드 검색", description = "진료명 키워드로 진료 코드를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "검색어 누락")
    })
    @GetMapping("/clinics")
    public ResponseEntity<ClinicListResponseDTO> getClinicNameAndCode(
            @Parameter(description = "진료명 검색어", required = true) @RequestParam("name") @NotBlank String name) {
        return ResponseEntity.ok(homeService.getClinicNameAndCode(name));
    }

    @Operation(summary = "지역 검색", description = "지역명 키워드로 시도/시군구 코드를 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "검색어 누락")
    })
    @GetMapping("/locations")
    public ResponseEntity<LocationListResponseDTO> getLocation(
            @Parameter(description = "지역명 검색어", required = true) @RequestParam("name") @NotBlank String name) {
        return ResponseEntity.ok(homeService.getLocationNameAndCode(name));
    }

    @Operation(summary = "병원명 검색", description = "지역과 병원명 키워드로 병원을 검색합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공"),
            @ApiResponse(responseCode = "400", description = "검색어 누락")
    })
    @GetMapping("/hospitals")
    public ResponseEntity<HospitalResponseDTO> getHospitalNameList(
            @Parameter(description = "지역 코드", required = true) @RequestParam("location") @NotBlank String location,
            @Parameter(description = "병원명 검색어", required = true) @RequestParam("name") @NotBlank String name) {
        return ResponseEntity.ok(homeService.getHospitalName(location, name));
    }
}
