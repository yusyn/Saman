package com.car.rental.service;

import com.car.rental.db.AppUserRepository;
import com.car.rental.db.AppUserRepository.AdminAccountRow;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class AdminAccountService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionRegistry sessions;

    public AdminAccountService(AppUserRepository users, PasswordEncoder passwordEncoder, SessionRegistry sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessions = sessions;
    }

    public List<AdminAccount> list() {
        return users.findAdminAccounts().stream().map(AdminAccountService::safeAccount).toList();
    }

    public AdminAccount create(String actor, String username, String password) {
        String name = validateUsername(username);
        validatePassword(password);
        String hash = passwordEncoder.encode(password);
        try {
            return users.inAdminMutation(actor, () -> {
                if (users.usernameExists(name)) throw conflict("این نام کاربری قبلاً ثبت شده است");
                users.createAdmin(name, hash);
                return users.findAdminAccounts().stream().filter(row -> row.username().equals(name))
                        .findFirst().map(AdminAccountService::safeAccount).orElseThrow();
            });
        } catch (DataIntegrityViolationException ex) {
            throw conflict("این نام کاربری قبلاً ثبت شده است");
        }
    }

    public AdminAccountMutation rename(String actor, String currentUsername, long id, String requestedUsername) {
        String name = validateUsername(requestedUsername);
        try {
            RenamedAccount result = users.inAdminMutation(actor, () -> {
                AdminAccountRow target = requireAccount(id);
                if (target.username().equals(name)) throw badRequest("نام کاربری تغییری نکرده است");
                if (users.usernameExists(name)) throw conflict("این نام کاربری قبلاً ثبت شده است");
                if (users.renameAdmin(id, name) != 1) throw notFound();
                return new RenamedAccount(target.username(),
                        safeAccount(users.findAdminById(id).orElseThrow(AdminAccountService::notFound)));
            });
            expireSessions(result.previousUsername());
            return new AdminAccountMutation(result.account(), result.previousUsername().equals(currentUsername));
        } catch (DataIntegrityViolationException ex) {
            throw conflict("این نام کاربری قبلاً ثبت شده است");
        }
    }

    public boolean changePassword(String actor, String currentUsername, long id, String password) {
        validatePassword(password);
        String hash = passwordEncoder.encode(password);
        String targetUsername = users.inAdminMutation(actor, () -> {
            AdminAccountRow target = requireAccount(id);
            users.changeAdminPassword(id, hash);
            return target.username();
        });
        expireSessions(targetUsername);
        return targetUsername.equals(currentUsername);
    }

    public AdminAccount setEnabled(String actor, String currentUsername, long id, boolean enabled) {
        AdminAccount account = users.inAdminMutation(actor, () -> {
            AdminAccountRow target = requireAccount(id);
            if (target.enabled() == enabled) throw badRequest(enabled ? "این حساب از قبل فعال است" : "این حساب از قبل غیرفعال است");
            if (!enabled && target.username().equals(currentUsername))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "نمی‌توانید حسابی را که با آن وارد شده‌اید غیرفعال کنید");
            if (!enabled && users.countActiveAdmins() <= 1)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "غیرفعال‌کردن آخرین مدیر فعال مجاز نیست");
            if (users.setAdminEnabled(id, enabled) != 1) throw notFound();
            return safeAccount(users.findAdminById(id).orElseThrow(AdminAccountService::notFound));
        });
        if (!enabled) expireSessions(account.username());
        return account;
    }

    private void expireSessions(String username) {
        for (Object principal : sessions.getAllPrincipals()) {
            if (principal instanceof org.springframework.security.core.userdetails.UserDetails details
                    && details.getUsername().equals(username)) {
                for (SessionInformation session : sessions.getAllSessions(principal, false)) session.expireNow();
            }
        }
    }

    private AdminAccountRow requireAccount(long id) {
        return users.findAdminById(id).orElseThrow(AdminAccountService::notFound);
    }

    private static String validateUsername(String username) {
        if (username == null || username.isBlank()) throw badRequest("نام کاربری الزامی است");
        String value = username.strip();
        if (value.length() > 120 || value.chars().anyMatch(Character::isISOControl))
            throw badRequest("نام کاربری معتبر نیست");
        return value;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8)
            throw badRequest("رمز عبور باید حداقل ۸ نویسه باشد");
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw badRequest("رمز عبور نباید بیش از ۷۲ بایت باشد");
    }

    private static AdminAccount safeAccount(AdminAccountRow row) {
        return new AdminAccount(row.id(), row.username(), row.enabled());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "حساب مدیر پیدا نشد");
    }

    public record AdminAccount(long id, String username, boolean enabled) {}
    public record AdminAccountMutation(AdminAccount account, boolean reauthenticationRequired) {}
    private record RenamedAccount(String previousUsername, AdminAccount account) {}
}
