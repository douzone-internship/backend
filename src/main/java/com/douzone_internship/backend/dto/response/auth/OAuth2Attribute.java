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
        }
        throw new IllegalArgumentException("Unknown Provider: " + provider);
    }

    public Users toEntity() {
        return Users.builder()
                .email(email)
                .name(name)
                .provider(Provider.valueOf(provider.toUpperCase()))
                .providerId((String) attributes.get(attributeKey))
                .build();
    }
}
