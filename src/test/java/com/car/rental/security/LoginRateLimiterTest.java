package com.car.rental.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {

    @Test
    @DisplayName("Blocks after 5 consecutive failed attempts")
    void testRateLimiterBlocks() {
        LoginRateLimiter limiter = new LoginRateLimiter(5, 60_000);
        String ip = "192.168.1.50";

        for (int i = 0; i < 5; i++) {
            assertFalse(limiter.isBlocked(ip));
            limiter.recordFailure(ip);
        }

        assertTrue(limiter.isBlocked(ip), "Should be blocked after 5 failed attempts");

        limiter.recordSuccess(ip);
        assertFalse(limiter.isBlocked(ip), "Success should reset the counter");
    }

    @Test
    @DisplayName("Attempts from different IPs are tracked independently")
    void testDifferentIpsIndependent() {
        LoginRateLimiter limiter = new LoginRateLimiter(3, 60_000);
        String ip1 = "10.0.0.1";
        String ip2 = "10.0.0.2";

        limiter.recordFailure(ip1);
        limiter.recordFailure(ip1);
        limiter.recordFailure(ip1);

        assertTrue(limiter.isBlocked(ip1));
        assertFalse(limiter.isBlocked(ip2));
    }

    @Test
    @DisplayName("Handles null and blank IP gracefully")
    void testNullOrBlankIpGraceful() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        assertFalse(limiter.isBlocked(null));
        assertFalse(limiter.isBlocked(""));
        assertFalse(limiter.isBlocked("   "));

        assertDoesNotThrow(() -> limiter.recordFailure(null));
        assertDoesNotThrow(() -> limiter.recordFailure(""));
        assertDoesNotThrow(() -> limiter.recordSuccess(null));
        assertDoesNotThrow(() -> limiter.recordSuccess(""));
    }

    @Test
    @DisplayName("Attempts expire after time window")
    void testWindowExpiration() throws InterruptedException {
        LoginRateLimiter limiter = new LoginRateLimiter(2, 50); // 50ms window
        String ip = "192.168.1.99";

        limiter.recordFailure(ip);
        limiter.recordFailure(ip);
        assertTrue(limiter.isBlocked(ip));

        Thread.sleep(70);
        assertFalse(limiter.isBlocked(ip), "Window should have expired old attempts");
    }
}
