package com.douzone_internship.backend.config;

import com.douzone_internship.backend.auth.JwtFilter;
import com.douzone_internship.backend.auth.JwtTokenProvider;
import com.douzone_internship.backend.auth.OAuth2SuccessHandler;
import com.douzone_internship.backend.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtTokenProvider tokenProvider;
        private final UserDetailsService userDetailsService;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        @Value("${app.cors.allowed-origins}")
        private String allowedOrigins;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 설정 추가
                                .csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 비활성화
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // OAuth2를 위해
                                                                                                           // 세션 사용
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**", "/oauth2/**", "/login/**",
                                                                "/api/home/**", "/home/**", "/api/result/**",
                                                                "/result/**",
                                                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                                                "/swagger-resources/**")
                                                .permitAll() // 인증, 홈, 결과, Swagger 관련 경로 모두 허용
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/comments")
                                                .permitAll() // 병원별 댓글 조회는 비로그인 허용
                                                .anyRequest().authenticated())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        // REST API - 인증 실패 시 리다이렉트 대신 401 반환
                                                        response.setContentType("application/json");
                                                        response.setStatus(401);
                                                        response.getWriter().write(
                                                                        "{\"code\":401,\"message\":\"Authentication required\"}");
                                                }))
                                .oauth2Login(oauth2 -> oauth2
                                        .authorizationEndpoint(authorization ->
                                                authorization.baseUri("/api/oauth2/authorization")
                                        )
                                        .redirectionEndpoint(redirection ->
                                                redirection.baseUri("/api/login/oauth2/code/*")
                                        )
                                        // (/login/oauth2/code/*) - Google/Kakao Console 등록 URI와 일치
                                        .userInfoEndpoint(userInfo -> userInfo
                                                .userService(customOAuth2UserService))
                                        .successHandler(oAuth2SuccessHandler))
                                .logout(logout -> logout
                                                .logoutUrl("/api/auth/logout")
                                                .logoutSuccessHandler((request, response, authentication) -> {
                                                        response.setStatus(200);
                                                        response.setContentType("application/json");
                                                        response.getWriter().write("{\"message\":\"로그아웃 성공\"}");
                                                })
                                                .deleteCookies("JSESSIONID")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true))
                                .addFilterBefore(new JwtFilter(tokenProvider, userDetailsService),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // 환경별 허용 origin 설정 (application.yaml의 app.cors.allowed-origins)
                List<String> origins = Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList();
                configuration.setAllowedOrigins(origins);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true); // 쿠키/세션 사용
                configuration.setExposedHeaders(List.of("*"));
                configuration.setMaxAge(3600L); // preflight 요청 캐시 시간

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
