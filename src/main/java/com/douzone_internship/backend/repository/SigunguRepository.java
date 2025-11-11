package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Sigungu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SigunguRepository extends JpaRepository<Sigungu, String> {
    List<Sigungu> findByNameContainingIgnoreCase(String name);
}
