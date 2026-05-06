package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.request.LoginRequestDTO;
import com.douzone_internship.backend.dto.request.SignupRequestDTO;
import com.douzone_internship.backend.dto.response.TokenResponseDTO;
import com.douzone_internship.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API (로그인, 회원가입, 회원정보)")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인합니다. Access/Refresh 토큰을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
            @ApiResponse(responseCode = "400", description = "이메일/비밀번호 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        TokenResponseDTO tokens = authService.login(request.email(), request.password());
        return ResponseEntity.ok(tokens);
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access/Refresh Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "403", description = "유효하지 않은 Refresh Token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDTO> refresh(@RequestBody Map<String, String> request) {
        TokenResponseDTO tokens = authService.refreshAccessToken(request.get("refreshToken"));
        return ResponseEntity.ok(tokens);
    }

    @Operation(summary = "로그아웃", description = "Access Token을 블랙리스트에 등록하고 Refresh Token을 삭제합니다.")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String accessToken = bearerToken.substring(7);
            authService.logout(accessToken, userDetails.getUsername());
        }
        return ResponseEntity.ok(Map.of("message", "로그아웃 성공"));
    }

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름으로 회원가입합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequestDTO request) {
        authService.signup(request.email(), request.password(), request.name());
        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }

    @Operation(summary = "현재 사용자 조회", description = "현재 인증된 사용자의 정보를 반환합니다.")
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 인증된 사용자의 계정을 삭제합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal Object principal) {
        authService.deleteAccount(principal);
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
    }
}
