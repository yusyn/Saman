package com.car.rental.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Session-based Spring Security for same-origin Web UI + REST API.
 * <ul>
 *   <li>Public: static UI, health, and read-only GET API</li>
 *   <li>Protected: all POST/PUT/PATCH/DELETE (authenticated + CSRF)</li>
 *   <li>Auth endpoints: login/logout/csrf/me</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfRepo.setCookiePath("/");

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(requestHandler)
                )
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(sf -> sf.migrateSession())
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.svg",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/icons/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/auth/me").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/fingerprint/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/employees/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/rentals/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
