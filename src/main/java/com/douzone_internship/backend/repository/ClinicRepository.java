package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, String> {

    // clinic_cd 컬럼에서 부분 일치 검색
    List<Clinic> findByNameContaining(String name);
}
