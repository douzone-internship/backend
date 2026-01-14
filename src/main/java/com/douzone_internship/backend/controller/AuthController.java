package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.auth.JwtTokenProvider;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.repository.UsersRepository;
import com.douzone_internship.backend.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsersRepository usersRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Users user = usersRepository.findAll().stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(password, user.getProviderId())) {

            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        String token = tokenProvider.createAccessToken(user.getEmail());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        if (usersRepository.findAll().stream().anyMatch(u -> u.getEmail().equals(email))) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        Users user = new Users();
        user.setEmail(email);
        user.setName(name);
        user.setProviderId(passwordEncoder.encode(password));
        usersRepository.save(user);

        return ResponseEntity.ok("회원가입 성공");
    }
}
