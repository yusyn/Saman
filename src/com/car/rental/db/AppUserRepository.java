package com.car.rental.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Persistence for application login accounts (separate from EmployeeTable / ZK device users).
 */
@Repository
public class AppUserRepository {

    private final JdbcTemplate jdbc;

    public AppUserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUserRow> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        var list = jdbc.query(
                "SELECT id, username, password_hash, role, enabled FROM AppUser WHERE username = ?",
                (rs, rowNum) -> new AppUserRow(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getInt("enabled") == 1
                ),
                username.strip()
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public long countUsers() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM AppUser", Long.class);
        return n == null ? 0L : n;
    }

    public void insert(String username, String passwordHash, String role) {
        jdbc.update(
                "INSERT INTO AppUser (username, password_hash, role, enabled) VALUES (?, ?, ?, 1)",
                username, passwordHash, role
        );
    }

    public record AppUserRow(long id, String username, String passwordHash, String role, boolean enabled) {
    }
}
