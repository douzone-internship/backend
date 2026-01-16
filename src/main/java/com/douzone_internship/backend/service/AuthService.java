package com.douzone_internship.backend.service;

import com.douzone_internship.backend.auth.JwtTokenProvider;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.repository.UsersRepository;
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
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public String login(String email, String password) {
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(password, user.getProviderId())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        return tokenProvider.createAccessToken(user.getEmail());
    }

    public void signup(String email, String password, String name) {
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        Users user = Users.builder()
                .email(email)
                .name(name)
                .providerId(passwordEncoder.encode(password))
                .build();

        usersRepository.save(user);
    }

    public Map<String, Object> getCurrentUser(Object principal) {
        if (principal == null) {
            return Map.of("authenticated", false);
        }
        String name = "";
        String email = "";

        if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            
            // Google 로그인의 경우
            if (oAuth2User.getAttribute("email") != null) {
                email = oAuth2User.getAttribute("email");
                name = oAuth2User.getAttribute("name");
            } 
            // Kakao 로그인의 경우 - 이메일이 없으므로 ID로 사용자 찾기
            else if (oAuth2User.getAttribute("id") != null) {
                Object kakaoIdObj = oAuth2User.getAttribute("id");
                String kakaoId = kakaoIdObj.toString();
                String generatedEmail = kakaoId + "@kakao.user";
                
                Users user = usersRepository.findByEmail(generatedEmail).orElse(null);
                if (user != null) {
                    email = user.getEmail();
                    name = user.getName();
                } else {
                    // 혹시 DB에 없으면 attributes에서 직접 가져오기
                    Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttribute("kakao_account");
                    if (kakaoAccount != null) {
                        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                        if (profile != null) {
                            name = (String) profile.get("nickname");
                        }
                    }
                    email = generatedEmail;
                }
            }
        } else if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            email = userDetails.getUsername();
            Users user = usersRepository.findByEmail(email).orElse(null);
            if (user != null) name = user.getName();
        }
        
        return Map.of(
                "authenticated", true,
                "name", name != null ? name : "",
                "email", email != null ? email : ""
        );
    }
}
