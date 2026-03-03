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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomeService {

        private final ClinicRepository clinicRepository;
        private final HospitalRepository hospitalRepository;
        private final SidoRepository sidoRepository;
        private final SigunguRepository sigunguRepository;

        @Transactional(readOnly = true)
        public ClinicListResponseDTO getClinicNameAndCode(String clinicName) {
                List<Clinic> clinics = clinicRepository.findByNameContaining(clinicName);

                List<ClinicResponseDTO> dtoList = clinics.stream()
                                .map(c -> ClinicResponseDTO.builder()
                                                .clinicCode(c.getClinicCd())
                                                .clinicName(c.getName())
                                                .build())
                                .toList();

                return ClinicListResponseDTO.builder()
                                .clinicResponseDTOList(dtoList)
                                .build();
        }

        @Transactional(readOnly = true)
        public LocationListResponseDTO getLocationNameAndCode(String name) {
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

                List<LocationResponseDTO> merged = new ArrayList<>();
                merged.addAll(sidoDtos);
                merged.addAll(sigunguDtos);

                return LocationListResponseDTO.of(merged);
        }

        @Transactional(readOnly = true)
        public HospitalResponseDTO getHospitalName(String location, String hospitalName) {
                Sigungu sigungu = sigunguRepository.getReferenceById(location);
                List<Hospital> hospitals = hospitalRepository.findBySigunguAndNameContainingIgnoreCase(sigungu,
                                hospitalName);
                if (hospitals.isEmpty()) {
                        hospitals = hospitalRepository.findBySigungu_Sido_SidoCdAndNameContainingIgnoreCase(location,
                                        hospitalName);
                }

                List<String> nameList = hospitals.stream()
                                .map(Hospital::getName)
                                .distinct()
                                .toList();

                return HospitalResponseDTO.builder()
                                .nameList(nameList)
                                .build();
        }
}
