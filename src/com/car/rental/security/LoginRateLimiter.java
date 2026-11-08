package com.car.rental.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class LoginRateLimiter {

    private final int maxAttempts;
    private final long windowMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> attempts = new ConcurrentHashMap<>();

    public LoginRateLimiter() {
        this(5, 5 * 60 * 1000L); // 5 attempts per 5 minutes
    }

    public LoginRateLimiter(int maxAttempts, long windowMillis) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMillis;
    }

    public boolean isBlocked(String key) {
        if (key == null || key.isBlank()) return false;
        ConcurrentLinkedQueue<Long> timestamps = attempts.get(key);
        if (timestamps == null) return false;

        long cutoff = Instant.now().toEpochMilli() - windowMillis;
        timestamps.removeIf(t -> t < cutoff);
        return timestamps.size() >= maxAttempts;
    }

    public void recordFailure(String key) {
        if (key == null || key.isBlank()) return;
        attempts.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>())
                .add(Instant.now().toEpochMilli());
    }

    public void recordSuccess(String key) {
        if (key != null) attempts.remove(key);
    }
}
