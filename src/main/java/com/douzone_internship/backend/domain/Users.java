package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;
    private String name;

    // 소셜 로그인 구분을 위한 필드
    @Enumerated(EnumType.STRING)
    private Provider provider; // GOOGLE, KAKAO

    private String providerId; // 소셜 서비스에서 준 고유 식별자


    public Users update(String name) {
        this.name = name;
        return this;
    }
}
