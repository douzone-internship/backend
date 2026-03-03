package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Favorite;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.dto.request.FavoriteRequestDTO;
import com.douzone_internship.backend.dto.response.FavoriteResponseDTO;
import com.douzone_internship.backend.exceptions.DuplicateResourceException;
import com.douzone_internship.backend.exceptions.ResourceNotFoundException;
import com.douzone_internship.backend.exceptions.UnauthorizedException;
import com.douzone_internship.backend.repository.FavoriteRepository;
import com.douzone_internship.backend.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

        private final FavoriteRepository favoriteRepository;
        private final UsersRepository usersRepository;

        @Transactional
        public FavoriteResponseDTO addFavorite(String userEmail, FavoriteRequestDTO dto) {
                Users user = findUserByEmail(userEmail);

                if (favoriteRepository.existsByUserIdAndHospitalNameAndClinicCode(user.getId(),
                                dto.getHospitalName().trim(),
                                dto.getClinicCode().trim())) {
                        throw new DuplicateResourceException("이미 찜한 항목입니다.");
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
                Users user = findUserByEmail(userEmail);

                Favorite favorite = favoriteRepository.findById(favoriteId)
                                .orElseThrow(() -> new ResourceNotFoundException("찜한 항목을 찾을 수 없습니다."));

                if (!favorite.getUser().getId().equals(user.getId())) {
                        throw new UnauthorizedException("본인의 찜 목록만 삭제할 수 있습니다.");
                }

                favoriteRepository.delete(favorite);
        }

        @Transactional
        public void removeFavoriteByDetails(String userEmail, String hospitalName, String clinicCode) {
                Users user = findUserByEmail(userEmail);

                Favorite favorite = favoriteRepository
                                .findByUserIdAndHospitalNameAndClinicCode(user.getId(), hospitalName.trim(),
                                                clinicCode.trim())
                                .orElseThrow(() -> new ResourceNotFoundException("찜한 항목을 찾을 수 없습니다."));

                favoriteRepository.delete(favorite);
        }

        public List<FavoriteResponseDTO> getFavorites(String userEmail) {
                Users user = findUserByEmail(userEmail);

                return favoriteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                                .map(FavoriteResponseDTO::from)
                                .toList();
        }

        public boolean isFavorited(String userEmail, String hospitalName, String clinicCode) {
                Users user = findUserByEmail(userEmail);

                return favoriteRepository.existsByUserIdAndHospitalNameAndClinicCode(user.getId(), hospitalName.trim(),
                                clinicCode.trim());
        }

        private Users findUserByEmail(String email) {
                return usersRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));
        }
}
