package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.UserRepository;
import id.pasim.mengaji.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder passwords, JwtService jwt) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody ApiDtos.RegisterRequest r) {
        if (r.requestedRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pendaftaran Admin tidak tersedia");
        }
        if (users.existsByEmail(r.email().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email sudah terdaftar");
        }
        var u = users.save(new UserAccount(
                r.name().trim(),
                r.email(),
                passwords.encode(r.password()),
                Role.ANGGOTA,
                r.requestedRole(),
                RegistrationStatus.PENDING
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Pendaftaran menunggu persetujuan", "user", ApiDtos.UserView.of(u)));
    }

    @PostMapping("/login")
    public ApiDtos.AuthResponse login(@Valid @RequestBody ApiDtos.LoginRequest r) {
        var u = users.findByEmail(r.email().toLowerCase()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email atau password salah"));
        if (!passwords.matches(r.password(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email atau password salah");
        }
        if (u.getStatus() != RegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akun belum disetujui oleh Admin/Pengurus");
        }
        return new ApiDtos.AuthResponse(jwt.create(u), ApiDtos.UserView.of(u));
    }

    @GetMapping("/me")
    public ApiDtos.UserView me(jakarta.servlet.http.HttpServletRequest req) {
        return ApiDtos.UserView.of(CurrentUser.get(req));
    }

    // Req 16: Forgot password via Email simulation
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ApiDtos.ForgotPasswordRequest r) {
        var u = users.findByEmail(r.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email tidak terdaftar dalam sistem"));

        String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant expiry = Instant.now().plus(30, ChronoUnit.MINUTES);
        u.setResetToken(token, expiry);
        users.save(u);

        return Map.of(
                "message", "Kode reset kata sandi telah dikirimkan ke email " + r.email(),
                "token", token,
                "email", r.email(),
                "expiresInMinutes", 30
        );
    }

    // Req 16: Reset password using token
    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@Valid @RequestBody ApiDtos.ResetPasswordRequest r) {
        var all = users.findAll();
        var user = all.stream()
                .filter(u -> r.token().trim().equalsIgnoreCase(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kode token reset tidak valid atau sudah kedaluwarsa"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kode token reset sudah kedaluwarsa. Silakan minta ulang.");
        }

        user.setPasswordHash(passwords.encode(r.newPassword()));
        user.setResetToken(null, null);
        users.save(user);

        return Map.of("message", "Kata sandi berhasil diatur ulang. Silakan masuk kembali dengan kata sandi baru.");
    }
}
