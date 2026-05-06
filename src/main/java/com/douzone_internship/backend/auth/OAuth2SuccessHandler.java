package com.douzone_internship.backend.auth;

import com.douzone_internship.backend.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.oauth2.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {

        org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) authentication;
        String email = null;

        log.info("OAuth2 Success Handler - Attributes: {}", oauthToken.getPrincipal().getAttributes());

        if ("google".equals(oauthToken.getAuthorizedClientRegistrationId())) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else if ("kakao".equals(oauthToken.getAuthorizedClientRegistrationId())) {
            java.util.Map<String, Object> kakaoAccount = oauthToken.getPrincipal().getAttribute("kakao_account");
            if (kakaoAccount != null) {
                email = (String) kakaoAccount.get("email");
            }
        }

        if (email == null) {
            log.error("Email not found in OAuth2 attributes");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from OAuth provider");
            return;
        }

        String token = tokenProvider.createAccessToken(email);
        String refreshToken = tokenProvider.createRefreshToken(email);
        refreshTokenService.saveRefreshToken(email, refreshToken);

        String redirectUrl = frontendRedirectUri + "?token=" + token + "&refreshToken=" + refreshToken;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}