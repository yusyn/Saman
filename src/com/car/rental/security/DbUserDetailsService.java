package com.car.rental.security;

import com.car.rental.db.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public DbUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return users.findByUsername(username)
                .map(row -> User.builder()
                        .username(row.username())
                        .password(row.passwordHash())
                        .disabled(!row.enabled())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + normalizeRole(row.role()))))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "ADMIN";
        }
        String r = role.strip().toUpperCase();
        if (r.startsWith("ROLE_")) {
            r = r.substring(5);
        }
        return r;
    }
}
