package com.douzone_internship.backend.dto.response.auth;

import com.douzone_internship.backend.domain.Provider;
import com.douzone_internship.backend.domain.Users;
import java.util.Map;
import lombok.Builder;

@Builder
public record OAuth2Attribute(
        Map<String, Object> attributes,
        String attributeKey,
        String email,
        String name,
        String provider
) {
    public static OAuth2Attribute of(String provider, Map<String, Object> attributes) {
        if ("google".equals(provider)) {
            return OAuth2Attribute.builder()
                    .email((String) attributes.get("email"))
                    .name((String) attributes.get("name"))
                    .attributes(attributes)
                    .attributeKey("sub")
                    .provider(provider)
                    .build();
        } else if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount == null) {
                throw new IllegalArgumentException("Kakao account information is missing");
            }
            
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            String email = (String) kakaoAccount.get("email");
            String nickname = profile != null ? (String) profile.get("nickname") : null;
            
            // 이메일이 없으면 카카오 ID로 대체
            if (email == null || email.isEmpty()) {
                String kakaoId = String.valueOf(attributes.get("id"));
                email = kakaoId + "@kakao.user";
            }
            
            // 닉네임이 없으면 기본값 사용
            if (nickname == null || nickname.isEmpty()) {
                nickname = "카카오 사용자";
            }
            
            return OAuth2Attribute.builder()
                    .email(email)
                    .name(nickname)
                    .attributes(attributes)
                    .attributeKey("id")
                    .provider(provider)
                    .build();
        }
        throw new IllegalArgumentException("Unknown Provider: " + provider);
    }

    public Users toEntity() {
        return Users.builder()
                .email(email)
                .name(name)
                .provider(Provider.valueOf(provider.toUpperCase()))
                .providerId(String.valueOf(attributes.get(attributeKey)))
                .build();
    }
}
