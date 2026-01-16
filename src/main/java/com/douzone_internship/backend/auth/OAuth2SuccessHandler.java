package com.douzone_internship.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        
        // 프론트엔드 콜백 페이지로 리다이렉트
        // 프론트엔드에서 세션 스토리지의 경로를 확인하고 리다이렉트 처리
        String redirectUrl = "http://localhost:3000/oauth/callback";
        
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}