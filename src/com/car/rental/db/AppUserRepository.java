package com.car.rental.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.List;
import java.util.function.Supplier;

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

    public List<AdminAccountRow> findAdminAccounts() {
        return jdbc.query("SELECT id, username, role, enabled FROM AppUser " +
                        "WHERE UPPER(REPLACE(role,'ROLE_',''))='ADMIN' ORDER BY username COLLATE NOCASE",
                (rs, rowNum) -> new AdminAccountRow(rs.getLong("id"), rs.getString("username"),
                        rs.getString("role"), rs.getInt("enabled") == 1));
    }

    public Optional<AdminAccountRow> findAdminById(long id) {
        var rows = jdbc.query("SELECT id, username, role, enabled FROM AppUser " +
                        "WHERE id=? AND UPPER(REPLACE(role,'ROLE_',''))='ADMIN'",
                (rs, rowNum) -> new AdminAccountRow(rs.getLong("id"), rs.getString("username"),
                        rs.getString("role"), rs.getInt("enabled") == 1), id);
        return rows.stream().findFirst();
    }

    public long countActiveAdmins() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM AppUser WHERE enabled=1 " +
                "AND UPPER(REPLACE(role,'ROLE_',''))='ADMIN'", Long.class);
        return n == null ? 0L : n;
    }

    public boolean usernameExists(String username) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM AppUser WHERE username=?", Integer.class, username);
        return n != null && n > 0;
    }

    /** Runs account checks and mutations on one transaction/connection; SQLite audit triggers join it. */
    public <T> T inAdminMutation(String actor, Supplier<T> work) {
        return transactions.execute(status -> {
            setAuditActor(actor);
            try {
                return work.get();
            } finally {
                setAuditActor(null);
            }
        });
    }

    public void createAdmin(String username, String passwordHash) {
        jdbc.update("INSERT INTO AppUser (username,password_hash,role,enabled) VALUES (?,?,'ADMIN',1)",
                username, passwordHash);
    }

    public int renameAdmin(long id, String username) {
        return jdbc.update("UPDATE AppUser SET username=? WHERE id=? AND UPPER(REPLACE(role,'ROLE_',''))='ADMIN'", username, id);
    }

    public int changeAdminPassword(long id, String passwordHash) {
        return jdbc.update("UPDATE AppUser SET password_hash=? WHERE id=? AND UPPER(REPLACE(role,'ROLE_',''))='ADMIN'", passwordHash, id);
    }

    public int setAdminEnabled(long id, boolean enabled) {
        return jdbc.update("UPDATE AppUser SET enabled=? WHERE id=? AND UPPER(REPLACE(role,'ROLE_',''))='ADMIN'",
                enabled ? 1 : 0, id);
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
            setAuditActor(currentActor());
            try {
                mutation.run();
            } finally {
                setAuditActor(null);
            }
        });
    }

    private void setAuditActor(String actor) {
        jdbc.update("UPDATE AppUserAuditContext SET actor=? WHERE id=1", actor);
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
    public record AdminAccountRow(long id, String username, String role, boolean enabled) {
    }
}
