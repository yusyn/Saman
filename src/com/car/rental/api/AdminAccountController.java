package com.car.rental.api;

import com.car.rental.service.AdminAccountService;
import com.car.rental.service.AdminAccountService.AdminAccount;
import com.car.rental.service.AdminAccountService.AdminAccountMutation;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin-users")
public class AdminAccountController {
    private final AdminAccountService accounts;

    public AdminAccountController(AdminAccountService accounts) {
        this.accounts = accounts;
    }

    public record CreateRequest(String username, String password) {}
    public record UsernameRequest(String username) {}
    public record PasswordRequest(String password) {}
    public record EnabledRequest(Boolean enabled) {}
    public record PasswordChangedResponse(String message, boolean reauthenticationRequired) {}

    @GetMapping
    public List<AdminAccount> list() {
        return accounts.list();
    }

    @PostMapping
    public AdminAccount create(@RequestBody(required = false) CreateRequest body, Authentication auth) {
        if (body == null) throw new IllegalArgumentException("نام کاربری و رمز عبور الزامی است");
        return accounts.create(auth.getName(), body.username(), body.password());
    }

    @PatchMapping("/{id}/username")
    public AdminAccountMutation rename(@PathVariable long id, @RequestBody(required = false) UsernameRequest body,
                                       Authentication auth) {
        if (body == null) throw new IllegalArgumentException("نام کاربری الزامی است");
        return accounts.rename(auth.getName(), auth.getName(), id, body.username());
    }

    @PatchMapping("/{id}/password")
    public PasswordChangedResponse changePassword(@PathVariable long id,
                                                  @RequestBody(required = false) PasswordRequest body,
                                                  Authentication auth) {
        if (body == null) throw new IllegalArgumentException("رمز عبور الزامی است");
        boolean reauth = accounts.changePassword(auth.getName(), auth.getName(), id, body.password());
        return new PasswordChangedResponse("رمز عبور حساب مدیر تغییر کرد", reauth);
    }

    @PatchMapping("/{id}/enabled")
    public AdminAccount setEnabled(@PathVariable long id, @RequestBody(required = false) EnabledRequest body,
                                   Authentication auth) {
        if (body == null || body.enabled() == null) throw new IllegalArgumentException("وضعیت حساب الزامی است");
        return accounts.setEnabled(auth.getName(), auth.getName(), id, body.enabled());
    }
}
