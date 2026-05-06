package com.douzone_internship.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class RefreshTokenService {

    @Value("${security.jwt.token.refresh-expiration}")
    private long refreshExpiration;

    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();
    private final Map<String, Long> blacklistStore = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "token-cleanup");
        t.setDaemon(true);
        return t;
    });

    public RefreshTokenService() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredBlacklist, 10, 10, TimeUnit.MINUTES);
    }

    public void saveRefreshToken(String email, String refreshToken) {
        refreshTokenStore.put(email, refreshToken);
    }

    public String getRefreshToken(String email) {
        return refreshTokenStore.get(email);
    }

    public void deleteRefreshToken(String email) {
        refreshTokenStore.remove(email);
    }

    public void blacklistAccessToken(String accessToken, long remainingMillis) {
        if (remainingMillis > 0) {
            blacklistStore.put(accessToken, System.currentTimeMillis() + remainingMillis);
        }
    }

    public boolean isBlacklisted(String token) {
        Long expiresAt = blacklistStore.get(token);
        if (expiresAt == null) return false;
        if (System.currentTimeMillis() > expiresAt) {
            blacklistStore.remove(token);
            return false;
        }
        return true;
    }

    private void cleanupExpiredBlacklist() {
        long now = System.currentTimeMillis();
        blacklistStore.entrySet().removeIf(entry -> now > entry.getValue());
    }
}
