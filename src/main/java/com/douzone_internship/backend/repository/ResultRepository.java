package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Result;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultRepository extends JpaRepository<Result, UUID> {
}
