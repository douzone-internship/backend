package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.SearchLog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, UUID> {
    List<SearchLog> findSearchLogBySearchKeyword(String searchKeyword);

    boolean existsSearchLogBySearchKeyword(String searchKeyword);

    Optional<SearchLog> findBySearchKeyword(String searchKeyword);
}
