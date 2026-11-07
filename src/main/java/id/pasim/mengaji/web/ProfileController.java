package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.UserAccount;
import id.pasim.mengaji.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ApiDtos.UserView getProfile(HttpServletRequest req) {
        UserAccount actor = CurrentUser.get(req);
        return ApiDtos.UserView.of(actor);
    }

    @PutMapping
    public ApiDtos.UserView updateProfile(@Valid @RequestBody ApiDtos.ProfileUpdateRequest req, HttpServletRequest httpReq) {
        UserAccount actor = CurrentUser.get(httpReq);
        actor.updateProfile(req.name(), req.phone(), req.nim(), req.prodi(), req.bio(), req.avatarUrl());
        return ApiDtos.UserView.of(users.save(actor));
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@Valid @RequestBody ApiDtos.ChangePasswordRequest req, HttpServletRequest httpReq) {
        UserAccount actor = CurrentUser.get(httpReq);
        if (!passwordEncoder.matches(req.currentPassword(), actor.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kata sandi saat ini salah");
        }
        actor.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        users.save(actor);
        return Map.of("message", "Kata sandi berhasil diperbarui");
    }
}
