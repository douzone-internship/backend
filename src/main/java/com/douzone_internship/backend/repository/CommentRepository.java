package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByUserOrderByCreatedAtDesc(Users user);

    List<Comment> findByHospitalNameAndClinicCodeOrderByCreatedAtDesc(String hospitalName, String clinicCode);

    List<Comment> findByHospitalNameInAndClinicCodeOrderByCreatedAtDesc(
            List<String> hospitalNames, String clinicCode);
}
