package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Favorite;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.dto.request.FavoriteRequestDTO;
import com.douzone_internship.backend.dto.response.FavoriteResponseDTO;
import com.douzone_internship.backend.repository.FavoriteRepository;
import com.douzone_internship.backend.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UsersRepository usersRepository;

    @Transactional
    public FavoriteResponseDTO addFavorite(String userEmail, FavoriteRequestDTO dto) {

        Users user = usersRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (favoriteRepository.existsByUserIdAndHospitalNameAndClinicCode(user.getId(), dto.getHospitalName().trim(),
                dto.getClinicCode().trim())) {
            throw new IllegalArgumentException("Already favorited");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .hospitalName(dto.getHospitalName().trim())
                .clinicName(dto.getClinicName())
                .clinicCode(dto.getClinicCode().trim())
                .location(dto.getLocation())
                .minPrice(dto.getMinPrice())
                .maxPrice(dto.getMaxPrice())
                .build();

        Favorite savedFavorite = favoriteRepository.save(favorite);
        log.info("Successfully added favorite: {}", savedFavorite.getId());
        return FavoriteResponseDTO.from(savedFavorite);
    }

    @Transactional
    public void removeFavorite(String userEmail, UUID favoriteId) {
        Users user = usersRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new IllegalArgumentException("Favorite not found"));

        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        favoriteRepository.delete(favorite);
    }

    @Transactional
    public void removeFavoriteByDetails(String userEmail, String hospitalName, String clinicCode) {

        Users user = usersRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Favorite favorite = favoriteRepository
                .findByUserIdAndHospitalNameAndClinicCode(user.getId(), hospitalName.trim(), clinicCode.trim())
                .orElseThrow(
                        () -> new IllegalArgumentException("Favorite not found - " + hospitalName + "/" + clinicCode));

        favoriteRepository.delete(favorite);
    }

    public List<FavoriteResponseDTO> getFavorites(String userEmail) {
        Users user = usersRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(FavoriteResponseDTO::from)
                .collect(Collectors.toList());
    }

    public boolean isFavorited(String userEmail, String hospitalName, String clinicCode) {
        Users user = usersRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return favoriteRepository.existsByUserIdAndHospitalNameAndClinicCode(user.getId(), hospitalName.trim(),
                clinicCode.trim());
    }
}
