package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Hospital;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, UUID> {

    // 시군구 코드 + 병원명 일부
    List<Hospital> findBySigungu_SgguCdAndNameContainingIgnoreCase(String sgguCd, String name);

    // 시도 코드 + 병원명 일부
    List<Hospital> findBySigungu_Sido_SidoCdAndNameContainingIgnoreCase(String sidoCd, String name);

    Optional<Hospital> findFirstByNameAndSigungu_SgguCd(String name, String sigungu);

    Optional<Hospital> findFirstByName(String name);

}
