package com.car.rental.security;

import com.car.rental.db.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/**
 * Idempotent bootstrap of the first admin account from environment variables.
 * Never hard-codes credentials. Runs only when AppUser table is empty and both
 * SAMAN_ADMIN_USERNAME and SAMAN_ADMIN_PASSWORD are set.
 * <p>
 * Fail-closed: if no users exist and env vars are missing, the app starts but
 * no write endpoints can be used until an admin is provisioned.
 */
@Component
@Order(100)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = Logger.getLogger(AdminBootstrap.class.getName());

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminBootstrap(
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${saman.admin.username:}") String adminUsername,
            @Value("${saman.admin.password:}") String adminPassword
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername == null ? "" : adminUsername.strip();
        this.adminPassword = adminPassword == null ? "" : adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        long count = users.countUsers();
        if (count > 0) {
            log.info("AppUser table already has " + count + " account(s); bootstrap skipped");
            return;
        }
        if (adminUsername.isEmpty() || adminPassword.isEmpty()) {
            log.warning(
                    "No AppUser accounts and SAMAN_ADMIN_USERNAME / SAMAN_ADMIN_PASSWORD not set. "
                            + "Write endpoints will reject all requests until an admin is provisioned."
            );
            return;
        }
        if (adminPassword.length() < 8) {
            log.severe("SAMAN_ADMIN_PASSWORD must be at least 8 characters; bootstrap refused");
            return;
        }
        String hash = passwordEncoder.encode(adminPassword);
        users.insert(adminUsername, hash, "ADMIN");
        log.info("Bootstrapped initial admin user '" + adminUsername + "' (password not logged)");
    }
}
