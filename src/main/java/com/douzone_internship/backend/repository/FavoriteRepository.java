package com.douzone_internship.backend.repository;

import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.domain.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Favorite> findByUser(Users user);

    Optional<Favorite> findByUserIdAndHospitalNameAndClinicCode(UUID userId, String hospitalName, String clinicCode);

    boolean existsByUserIdAndHospitalNameAndClinicCode(UUID userId, String hospitalName, String clinicCode);
}
