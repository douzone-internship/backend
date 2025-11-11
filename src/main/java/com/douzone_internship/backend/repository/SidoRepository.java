package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Sido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SidoRepository extends JpaRepository<Sido, String> {
    List<Sido> findByNameContainingIgnoreCase(String name);
}
