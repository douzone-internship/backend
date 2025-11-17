package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.AiComment;

import java.util.Optional;
import java.util.UUID;

import com.douzone_internship.backend.domain.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiCommentRepository extends JpaRepository<AiComment, UUID> {
    Optional<AiComment> findBySearchLog(SearchLog searchLog);
}
