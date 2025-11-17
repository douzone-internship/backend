package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.SearchLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {
}
