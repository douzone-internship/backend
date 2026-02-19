package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.response.ClinicListResponseDTO;
import com.douzone_internship.backend.dto.response.HospitalResponseDTO;
import com.douzone_internship.backend.dto.response.LocationListResponseDTO;
import com.douzone_internship.backend.service.HomeService;
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
public class HomeController {

    private final HomeService homeService;

    // 진로명, 진료 코드 조회
    @GetMapping("/clinics")
    public ResponseEntity<ClinicListResponseDTO> getClinicNameAndCode(@RequestParam("name") @NotBlank String name) {
        return homeService.getClinicNameAndCode(name);
    }

    // 지역명, 시도 코드, 시군구 코드 조회
    @GetMapping("/locations")
    public ResponseEntity<LocationListResponseDTO> getLocation(@RequestParam("name") @NotBlank String name) {
        return homeService.getLocationNameAndCode(name);
    }

    // 병원명 조회
    @GetMapping("/hospitals")
    public ResponseEntity<HospitalResponseDTO> getHospitalNameList(
            @RequestParam("location") @NotBlank String location,
            @RequestParam("name") @NotBlank String name) {
        return homeService.getHospitalName(location, name);
    }

}
