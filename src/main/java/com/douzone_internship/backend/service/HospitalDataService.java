package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Hospital;
import com.douzone_internship.backend.domain.Sigungu;
import com.douzone_internship.backend.dto.response.RawHospitalResponseDTO;
import com.douzone_internship.backend.repository.HospitalRepository;
import com.douzone_internship.backend.repository.SigunguRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HospitalDataService extends AbstractApiService<RawHospitalResponseDTO, Hospital>{

    private final HospitalRepository hospitalRepository;
    private final SigunguRepository sigunguRepository;

    @Override
    protected Hospital convertDtoToEntity(RawHospitalResponseDTO dto) {
        Hospital hospital = new Hospital();
        hospital.setHospitalAddress(dto.getAddr());
        hospital.setHospitalUrl(dto.getHospUrl());
        hospital.setName(dto.getYadmNm());
        hospital.setYkiho(dto.getYkiho());

        Sigungu sigungu = sigunguRepository.getReferenceById(dto.getSgguCd());
        hospital.setSigungu(sigungu);
        return hospital;
    }

    @Override
    protected JpaRepository<Hospital, ?> getRepository() {
        return hospitalRepository;
    }

    @Transactional
    public void updateHospitals(String jsonResponse) {
        processApiResponse(jsonResponse, RawHospitalResponseDTO.class);
    }
}
