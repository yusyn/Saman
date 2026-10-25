package com.car.rental.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

/**
 * Persistence for application login accounts (separate from EmployeeTable / ZK device users).
 */
@Repository
public class AppUserRepository {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public AppUserRepository(JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactionManager);
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
        withAuditActor(() -> jdbc.update(
                "INSERT INTO AppUser (username, password_hash, role, enabled) VALUES (?, ?, ?, 1)",
                username, passwordHash, role));
    }

    /** Update through this repository so the SQLite trigger can attribute the change to the current principal. */
    public void update(long id, String username, String passwordHash, String role, boolean enabled) {
        withAuditActor(() -> jdbc.update(
                "UPDATE AppUser SET username=?, password_hash=?, role=?, enabled=? WHERE id=?",
                username, passwordHash, role, enabled ? 1 : 0, id));
    }

    public void delete(long id) {
        withAuditActor(() -> jdbc.update("DELETE FROM AppUser WHERE id=?", id));
    }

    private void withAuditActor(Runnable mutation) {
        transactions.executeWithoutResult(status -> {
            jdbc.update("UPDATE AppUserAuditContext SET actor=? WHERE id=1", currentActor());
            mutation.run();
            jdbc.update("UPDATE AppUserAuditContext SET actor=NULL WHERE id=1");
        });
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) return "سامانه";
        String name = authentication.getName();
        return name == null || name.isBlank() ? "سامانه" : name.strip().substring(0, Math.min(120, name.strip().length()));
    }

    public record AppUserRow(long id, String username, String passwordHash, String role, boolean enabled) {
    }
}
