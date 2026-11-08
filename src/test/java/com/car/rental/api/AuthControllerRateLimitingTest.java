package com.car.rental.api;

import com.car.rental.api.dto.ApiError;
import com.car.rental.security.LoginRateLimiter;
import com.car.rental.service.SystemEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.context.NullSecurityContextRepository;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerRateLimitingTest {

    private AuthenticationManager authenticationManager;
    private LoginRateLimiter rateLimiter;
    private AuthController controller;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private AtomicBoolean authCalled;

    static class StubSystemEventService extends SystemEventService {
        public StubSystemEventService() {
            super(null, null, null);
        }

        @Override
        public void recordAs(String actor, String category, String type, String result, String description, String entityType, Object entityId) {
            // no-op
        }
    }

    @BeforeEach
    void setUp() {
        authCalled = new AtomicBoolean(false);
        authenticationManager = auth -> {
            authCalled.set(true);
            if ("correct-password".equals(auth.getCredentials())) {
                return new UsernamePasswordAuthenticationToken(
                        auth.getPrincipal(),
                        auth.getCredentials(),
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
            }
            throw new BadCredentialsException("Bad credentials");
        };

        rateLimiter = new LoginRateLimiter(2, 60_000);

        controller = new AuthController(
                authenticationManager,
                new NullSecurityContextRepository(),
                new StubSystemEventService(),
                new SessionRegistryImpl(),
                rateLimiter
        );

        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Returns 429 when IP is blocked without querying authenticationManager")
    void testBlockedIpReturns429() {
        // Exceed attempts limit to block IP
        rateLimiter.recordFailure("192.168.1.100");
        rateLimiter.recordFailure("192.168.1.100");
        assertTrue(rateLimiter.isBlocked("192.168.1.100"));

        ResponseEntity<?> responseEntity = controller.login(
                new AuthController.LoginRequest("admin", "secret"),
                request,
                response
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, responseEntity.getStatusCode());
        assertTrue(responseEntity.getBody() instanceof ApiError);
        ApiError error = (ApiError) responseEntity.getBody();
        assertEquals(429, error.getStatus());
        assertFalse(authCalled.get(), "AuthenticationManager must not be called when rate limited");
    }

    @Test
    @DisplayName("Records failure on AuthenticationException")
    void testFailedLoginRecordsFailure() {
        assertFalse(rateLimiter.isBlocked("192.168.1.100"));

        ResponseEntity<?> responseEntity = controller.login(
                new AuthController.LoginRequest("admin", "wrong-password"),
                request,
                response
        );

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertTrue(authCalled.get());
        // Limit is 2; 1 failed attempt should not block yet
        assertFalse(rateLimiter.isBlocked("192.168.1.100"));
        // A second failure reaches limit
        rateLimiter.recordFailure("192.168.1.100");
        assertTrue(rateLimiter.isBlocked("192.168.1.100"));
    }

    @Test
    @DisplayName("Records success on valid authentication")
    void testSuccessfulLoginRecordsSuccess() {
        // Pre-record 1 failure
        rateLimiter.recordFailure("192.168.1.100");

        ResponseEntity<?> responseEntity = controller.login(
                new AuthController.LoginRequest("admin", "correct-password"),
                request,
                response
        );

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertTrue(authCalled.get());
        // Success should reset the counter, so 1 more failure must not block
        rateLimiter.recordFailure("192.168.1.100");
        assertFalse(rateLimiter.isBlocked("192.168.1.100"), "Success should have reset the attempt counter");
    }
}
