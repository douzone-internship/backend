package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Favorite;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.dto.response.FavoriteListDTO;
import com.douzone_internship.backend.repository.FavoriteRepository;
import com.douzone_internship.backend.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository usersRepository;
    private final FavoriteRepository favoriteRepository;

    public ResponseEntity<?> getUserFavorite(UUID userId) {
        try {
            Users user = usersRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            List<Favorite> favorites = favoriteRepository.findByUser(user);

            List<FavoriteListDTO> favoriteListDTOs = favorites.stream()
                    .map(FavoriteListDTO::from)
                    .toList();

            return ResponseEntity.ok(favoriteListDTOs);
        } catch (IllegalArgumentException e) {
            log.error("사용자 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            log.error("즐겨찾기 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body("즐겨찾기 조회 중 오류가 발생했습니다.");
        }
    }
}
