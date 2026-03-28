package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.request.FavoriteRequestDTO;
import com.douzone_internship.backend.dto.response.FavoriteResponseDTO;
import com.douzone_internship.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Favorite", description = "찜 관련 API")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "찜 추가", description = "병원을 찜 목록에 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 추가 성공"),
            @ApiResponse(responseCode = "409", description = "이미 찜한 항목")
    })
    @PostMapping
    public ResponseEntity<FavoriteResponseDTO> addFavorite(@AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FavoriteRequestDTO dto) {
        return ResponseEntity.ok(favoriteService.addFavorite(userDetails.getUsername(), dto));
    }

    @Operation(summary = "찜 삭제 (ID)", description = "찜 ID로 찜 목록에서 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 찜만 삭제 가능"),
            @ApiResponse(responseCode = "404", description = "찜 항목을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFavorite(@AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "찜 ID") @PathVariable UUID id) {
        favoriteService.removeFavorite(userDetails.getUsername(), id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "찜 삭제 (병원명+진료코드)", description = "병원명과 진료코드로 찜 목록에서 제거합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "찜 항목을 찾을 수 없음")
    })
    @DeleteMapping
    public ResponseEntity<Void> removeFavoriteByDetails(@AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "병원명", required = true) @RequestParam String hospitalName,
            @Parameter(description = "진료 코드", required = true) @RequestParam String clinicCode) {
        favoriteService.removeFavoriteByDetails(userDetails.getUsername(), hospitalName, clinicCode);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "찜 목록 조회", description = "로그인한 사용자의 찜 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<List<FavoriteResponseDTO>> getFavorites(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(favoriteService.getFavorites(userDetails.getUsername()));
    }

    @Operation(summary = "찜 여부 확인", description = "특정 병원+진료코드 조합이 이미 찜되어 있는지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "찜 여부 반환")
    })
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkFavorite(@AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "병원명", required = true) @RequestParam String hospitalName,
            @Parameter(description = "진료 코드", required = true) @RequestParam String clinicCode) {
        boolean result = favoriteService.isFavorited(userDetails.getUsername(), hospitalName, clinicCode);

        return ResponseEntity.ok(result);
    }
}
