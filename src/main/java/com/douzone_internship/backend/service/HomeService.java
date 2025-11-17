package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Clinic;
import com.douzone_internship.backend.domain.Hospital;
import com.douzone_internship.backend.domain.Sigungu;
import com.douzone_internship.backend.dto.response.ClinicListResponseDTO;
import com.douzone_internship.backend.dto.response.ClinicResponseDTO;
import com.douzone_internship.backend.dto.response.HospitalResponseDTO;
import com.douzone_internship.backend.dto.response.LocationListResponseDTO;
import com.douzone_internship.backend.dto.response.LocationResponseDTO;
import com.douzone_internship.backend.repository.ClinicRepository;
import com.douzone_internship.backend.repository.HospitalRepository;
import com.douzone_internship.backend.repository.SidoRepository;
import com.douzone_internship.backend.repository.SigunguRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeService {

    private final ClinicRepository clinicRepository;
    private final HospitalRepository hospitalRepository;
    private final SidoRepository sidoRepository;
    private final SigunguRepository sigunguRepository;

    // 진로명, 진료 코드 조회 비지니스 로직
    @Transactional(readOnly = true)
    public ResponseEntity<ClinicListResponseDTO> getClinicNameAndCode(String clinicName) {
        List<Clinic> clinics = clinicRepository.findByNameContaining(clinicName);

        List<ClinicResponseDTO> dtoList = clinics.stream()
                .map(c -> ClinicResponseDTO.builder()
                        .clinicCode(c.getClinicCd())
                        .clinicName(c.getName())
                        .build())
                .toList();

        ClinicListResponseDTO clinicListResponseDTO = ClinicListResponseDTO.builder()
                .clinicResponseDTOList(dtoList)
                .build();

        return ResponseEntity.ok(clinicListResponseDTO);
    }

    // 지역명 조회 (시도 + 시군구) - 이름 부분 일치)
    @Transactional(readOnly = true)
    public ResponseEntity<LocationListResponseDTO> getLocationNameAndCode(String name) {

        List<LocationResponseDTO> sidoDtos = sidoRepository.findByNameContainingIgnoreCase(name).stream()
                .map(s -> LocationResponseDTO.builder()
                        .locationName(s.getName())
                        .sidoCode(s.getSidoCd())
                        .sgguCode(null)
                        .build())
                .toList();

        List<LocationResponseDTO> sigunguDtos = sigunguRepository.findByNameContainingIgnoreCase(name).stream()
                .map(g -> LocationResponseDTO.builder()
                        .locationName(g.getName())
                        .sidoCode(g.getSido().getSidoCd())
                        .sgguCode(g.getSgguCd())
                        .build())
                .toList();

        List<LocationResponseDTO> merged = new java.util.ArrayList<>();
        merged.addAll(sidoDtos);
        merged.addAll(sigunguDtos);

        LocationListResponseDTO dto = LocationListResponseDTO.of(merged);
        return ResponseEntity.ok(dto);
    }

    // 병원명 조회 비지니스 로직
    @Transactional(readOnly = true)
    public ResponseEntity<HospitalResponseDTO> getHospitalName(String location, String hospitalName) {

        Sigungu sigungu = sigunguRepository.getReferenceById(location);
        List<Hospital> hospitals = hospitalRepository.findBySigunguAndNameContainingIgnoreCase(sigungu, hospitalName);
        if (hospitals.isEmpty()) {
            hospitals = hospitalRepository.findBySigungu_Sido_SidoCdAndNameContainingIgnoreCase(location, hospitalName);
        }

        List<String> nameList = hospitals.stream()
                .map(Hospital::getName)
                .distinct()
                .toList();
        HospitalResponseDTO dto = HospitalResponseDTO.builder()
                .nameList(nameList)
                .build();
        return ResponseEntity.ok(dto);
    }

}
