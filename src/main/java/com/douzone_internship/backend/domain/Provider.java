package com.douzone_internship.backend.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE, // 구글 소셜 로그인
    KAKAO,
}