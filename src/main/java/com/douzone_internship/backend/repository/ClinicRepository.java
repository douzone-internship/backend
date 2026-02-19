package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, String> {

    List<Clinic> findByNameContaining(String name);

    Clinic findByName(String name);
}
