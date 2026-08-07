package com.douzone_internship.backend.health;

import com.douzone_internship.backend.crontab.JsonScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component("hiraBatchFreshness")
@RequiredArgsConstructor
public class BatchFreshnessHealthIndicator implements HealthIndicator {

    private final JsonScheduler scheduler;

    private static final Duration STALE_THRESHOLD = Duration.ofDays(35);

    @Override
    public Health health() {
        Instant last = scheduler.getLastSuccessTime();

        if (last == null) {
            return Health.unknown()
                    .withDetail("reason", "배치가 아직 한 번도 성공하지 않음")
                    .withDetail("lastFailureTime", String.valueOf(scheduler.getLastFailureTime()))
                    .withDetail("lastFailureMessage", String.valueOf(scheduler.getLastFailureMessage()))
                    .build();
        }

        Duration since = Duration.between(last, Instant.now());
        boolean stale = since.compareTo(STALE_THRESHOLD) > 0;

        Health.Builder builder = stale ? Health.down() : Health.up();

        return builder
                .withDetail("lastSuccessTime", last.toString())
                .withDetail("ageInDays", since.toDays())
                .withDetail("thresholdDays", STALE_THRESHOLD.toDays())
                .build();
    }
}