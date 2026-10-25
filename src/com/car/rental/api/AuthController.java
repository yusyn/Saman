package com.car.rental.api;

import com.car.rental.api.dto.ApiError;
import com.car.rental.api.dto.OkResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.car.rental.service.SystemEventService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SystemEventService systemEvents;
    private final SessionRegistry sessionRegistry;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SystemEventService systemEvents,
            SessionRegistry sessionRegistry
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.systemEvents = systemEvents;
        this.sessionRegistry = sessionRegistry;
    }

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody(required = false) LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (body == null
                || body.username() == null || body.username().isBlank()
                || body.password() == null || body.password().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiError("نام کاربری و رمز عبور الزامی است", 400));
        }
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            body.username().strip(),
                            body.password()
                    )
            );
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            // This endpoint authenticates manually, so apply fixation protection and register
            // the session explicitly for later credential-change revocation.
            if (request.getSession(false) == null) request.getSession(true);
            request.changeSessionId();
            securityContextRepository.saveContext(context, request, response);
            sessionRegistry.registerNewSession(request.getSession(false).getId(), auth.getPrincipal());
            systemEvents.recordAs(auth.getName(), "ورود", "ورود مدیر", "موفق", "ورود مدیر با موفقیت انجام شد", "مدیر", auth.getName());

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("ok", true);
            payload.put("username", auth.getName());
            payload.put("roles", auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .collect(Collectors.toList()));
            return ResponseEntity.ok(payload);
        } catch (AuthenticationException ex) {
            systemEvents.recordAs(body.username().strip(), "ورود", "ورود مدیر", "ناموفق", "ورود مدیر ناموفق بود", "مدیر", body.username().strip());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiError("نام کاربری یا رمز عبور نادرست است", 401));
        }
    }

    @PostMapping("/logout")
    public OkResponse logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return OkResponse.ok("خروج انجام شد");
    }

    /**
     * Forces CSRF cookie issuance and returns the token for clients that cannot
     * read the XSRF-TOKEN cookie (or for explicit header use).
     */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token.getToken());
        body.put("headerName", token.getHeaderName());
        body.put("parameterName", token.getParameterName());
        return body;
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            body.put("authenticated", false);
            return body;
        }
        body.put("authenticated", true);
        body.put("username", authentication.getName());
        body.put("roles", authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList()));
        return body;
    }
}
