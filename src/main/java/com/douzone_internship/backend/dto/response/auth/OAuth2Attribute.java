package com.douzone_internship.backend.dto.response.auth;

import java.util.Map;

public record OAuth2Attribute(
        Map<String, Object> attributes,
        String attributeKey,
        String email,
        String name
) {
    public static OAuth2Attribute of(String provider, Map<String, Object> attributes) {
        if ("google".equals(provider)) {
            return new OAuth2Attribute(
                    attributes,
                    "sub",
                    (String) attributes.get("email"),
                    (String) attributes.get("name")
            );
        }
        throw new IllegalArgumentException("Unknown Provider");
    }
}