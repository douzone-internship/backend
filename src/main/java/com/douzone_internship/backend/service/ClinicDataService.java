package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Clinic;
import com.douzone_internship.backend.dto.response.RawClinicResponseDTO;
import com.douzone_internship.backend.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClinicDataService extends AbstractApiService<RawClinicResponseDTO, Clinic> {

    private final ClinicRepository clinicRepository;

    @Override
    protected Clinic convertDtoToEntity(RawClinicResponseDTO dto) {
        Clinic clinic = new Clinic();
        clinic.setClinicCd(dto.getNpayCd());
        clinic.setName(dto.getNpayKorNm());
        return clinic;
    }

    @Override
    protected JpaRepository<Clinic, ?> getRepository() {
        return clinicRepository;
    }

    @Transactional
    public void updateClinics(String jsonResponse) {
        processApiResponse(jsonResponse, RawClinicResponseDTO.class);
    }
}