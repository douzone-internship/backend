package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.request.LoginRequestDTO;
import com.douzone_internship.backend.dto.request.SignupRequestDTO;
import com.douzone_internship.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API (로그인, 회원가입, 회원정보)")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인합니다. JWT 토큰을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환"),
            @ApiResponse(responseCode = "400", description = "이메일/비밀번호 오류")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        String token = authService.login(request.email(), request.password());
        return ResponseEntity.ok(Map.of("token", token));
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 정보 반환")
    })
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Object principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }

    @Operation(summary = "회원 탈퇴", description = "현재 인증된 사용자의 계정을 삭제합니다. 찜 목록과 댓글도 함께 삭제됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal Object principal) {
        authService.deleteAccount(principal);
        return ResponseEntity.ok(Map.of("message", "회원 탈퇴가 완료되었습니다."));
    }
}
