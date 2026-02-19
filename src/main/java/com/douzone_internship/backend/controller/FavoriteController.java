package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.request.FavoriteRequestDTO;
import com.douzone_internship.backend.dto.response.FavoriteResponseDTO;
import com.douzone_internship.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping
    public ResponseEntity<FavoriteResponseDTO> addFavorite(@AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FavoriteRequestDTO dto) {
        return ResponseEntity.ok(favoriteService.addFavorite(userDetails.getUsername(), dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID id) {
        favoriteService.removeFavorite(userDetails.getUsername(), id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFavoriteByDetails(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String hospitalName,
            @RequestParam String clinicCode) {
        favoriteService.removeFavoriteByDetails(userDetails.getUsername(), hospitalName, clinicCode);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<FavoriteResponseDTO>> getFavorites(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(favoriteService.getFavorites(userDetails.getUsername()));
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkFavorite(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String hospitalName,
            @RequestParam String clinicCode) {

        boolean result = favoriteService.isFavorited(userDetails.getUsername(), hospitalName, clinicCode);

        return ResponseEntity.ok(result);
    }
}
