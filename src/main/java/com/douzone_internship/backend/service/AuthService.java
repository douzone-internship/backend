package com.douzone_internship.backend.service;

import com.douzone_internship.backend.auth.JwtTokenProvider;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.dto.response.TokenResponseDTO;
import com.douzone_internship.backend.exceptions.DuplicateResourceException;
import com.douzone_internship.backend.exceptions.ResourceNotFoundException;
import com.douzone_internship.backend.exceptions.UnauthorizedException;
import com.douzone_internship.backend.repository.UsersRepository;
import com.douzone_internship.backend.repository.CommentRepository;
import com.douzone_internship.backend.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UsersRepository usersRepository;
    private final FavoriteRepository favoriteRepository;
    private final CommentRepository commentRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public TokenResponseDTO login(String email, String password) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (user.getPassword() == null) {
            throw new IllegalArgumentException("소셜 로그인으로 가입된 계정입니다.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        return issueTokens(user.getEmail());
    }

    public TokenResponseDTO refreshAccessToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("유효하지 않은 Refresh Token입니다.");
        }

        String email = tokenProvider.getSubject(refreshToken);
        String storedToken = refreshTokenService.getRefreshToken(email);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new UnauthorizedException("Refresh Token이 일치하지 않습니다.");
        }

        return issueTokens(email);
    }

    public void logout(String accessToken, String email) {
        long remaining = tokenProvider.getRemainingExpiration(accessToken);
        refreshTokenService.blacklistAccessToken(accessToken, remaining);
        refreshTokenService.deleteRefreshToken(email);
    }

    private TokenResponseDTO issueTokens(String email) {
        String accessToken = tokenProvider.createAccessToken(email);
        String refreshToken = tokenProvider.createRefreshToken(email);
        refreshTokenService.saveRefreshToken(email, refreshToken);
        return new TokenResponseDTO(accessToken, refreshToken);
    }

    public void signup(String email, String password, String name) {
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("이미 가입된 이메일입니다.");
        }

        Users user = Users.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(password))
                .build();

        usersRepository.save(user);
    }

    public Map<String, Object> getCurrentUser(Object principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        String name = "";
        String email = "";
        String provider = "";

        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;

            if (oAuth2User.getAttribute("email") != null) {
                email = oAuth2User.getAttribute("email");
                name = oAuth2User.getAttribute("name");
                provider = "GOOGLE";
            } else if (oAuth2User.getAttribute("id") != null) {
                Object kakaoIdObj = oAuth2User.getAttribute("id");
                String kakaoId = kakaoIdObj.toString();
                String generatedEmail = kakaoId + "@kakao.user";

                Users user = usersRepository.findByEmail(generatedEmail).orElse(null);
                if (user != null) {
                    email = user.getEmail();
                    name = user.getName();
                } else {
                    Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttribute("kakao_account");
                    if (kakaoAccount != null) {
                        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                        if (profile != null) {
                            name = (String) profile.get("nickname");
                        }
                    }
                    email = generatedEmail;
                }
                provider = "KAKAO";
            }
        } else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            email = userDetails.getUsername();
            Users user = usersRepository.findByEmail(email).orElse(null);
            if (user != null) {
                name = user.getName();
                provider = user.getProvider() != null ? user.getProvider().name() : "";
            }
        }

        return Map.of(
                "authenticated", true,
                "name", name != null ? name : "",
                "email", email != null ? email : "",
                "provider", provider != null ? provider : "");
    }

    public void deleteAccount(Object principal) {
        String email = extractEmail(principal);
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        commentRepository.deleteAll(commentRepository.findByUserOrderByCreatedAtDesc(user));
        favoriteRepository.deleteByUser(user);
        usersRepository.delete(user);
    }

    private String extractEmail(Object principal) {
        if (principal instanceof OAuth2User oAuth2User) {
            if (oAuth2User.getAttribute("email") != null) {
                return oAuth2User.getAttribute("email");
            }
            if (oAuth2User.getAttribute("id") != null) {
                return oAuth2User.getAttribute("id").toString() + "@kakao.user";
            }
        } else if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new IllegalArgumentException("인증 정보를 확인할 수 없습니다.");
    }
}
