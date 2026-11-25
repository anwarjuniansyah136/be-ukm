package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.TahfidzAuditionRepository;
import id.pasim.mengaji.repo.UserRepository;
import id.pasim.mengaji.security.JwtService;
import id.pasim.mengaji.service.MailService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
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
    private final MailService mailService;
    private final TahfidzAuditionRepository auditions;

    public AuthController(
            UserRepository users,
            PasswordEncoder passwords,
            JwtService jwt,
            MailService mailService,
            TahfidzAuditionRepository auditions
    ) {
        this.users = users;
        this.passwords = passwords;
        this.jwt = jwt;
        this.mailService = mailService;
        this.auditions = auditions;
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody ApiDtos.RegisterRequest r) {
        if (r.requestedRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pendaftaran Admin tidak tersedia");
        }
        if (users.existsByEmail(r.email().toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email sudah terdaftar");
        }

        boolean isPengurusApplication = r.requestedRole() == Role.PENGURUS;
        ProgramType chosenProgram = isPengurusApplication
                ? null
                : (r.requestedProgram() != null ? r.requestedProgram() : ProgramType.TAHLIL);
        if (chosenProgram == ProgramType.HALAQAH) {
            chosenProgram = ProgramType.TAHLIL;
        }

        // Req 4: Pendaftaran awal ke program Tahfidz wajib tes bacaan 1-3 ayat (maksimal 3 ayat) random
        if (chosenProgram == ProgramType.TAHFIDZ) {
            if (r.testSurah() == null || r.testSurah().isBlank()
                    || r.testAyatStart() == null || r.testAyatEnd() == null
                    || r.testRecordingUrl() == null || r.testRecordingUrl().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Pendaftaran program Tahfidz wajib menyertakan hasil tes bacaan 1–3 ayat random beserta tautan rekaman audio/video."
                );
            }
            int ayatCount = r.testAyatEnd() - r.testAyatStart() + 1;
            if (r.testAyatStart() < 1 || ayatCount < 1 || ayatCount > 3) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tes bacaan Tahfidz harus terdiri dari 1 sampai 3 ayat (maksimal 3 ayat)."
                );
            }
        }

        // Saat pendaftaran awal, role belum ditetapkan (null) hingga disetujui Admin/Pengurus
        var u = new UserAccount(
                r.name().trim(),
                r.email(),
                passwords.encode(r.password()),
                null,
                r.requestedRole(),
                RegistrationStatus.PENDING
        );
        u.setRequestedProgram(chosenProgram);
        if (chosenProgram == ProgramType.TAHFIDZ) {
            u.updateMemberStatus(MemberStatus.MENUNGGU_TES_TAHFIDZ);
        } else {
            u.updateMemberStatus(MemberStatus.AKTIF_TAHLIL);
        }
        u = users.save(u);

        if (chosenProgram == ProgramType.TAHFIDZ) {
            var audition = new TahfidzAudition(
                    u,
                    r.testSurah().trim(),
                    r.testAyatStart(),
                    r.testAyatEnd(),
                    r.testRecordingUrl().trim(),
                    r.testNote()
            );
            auditions.save(audition);
        }

        String msg = isPengurusApplication
                ? "Pendaftaran Calon Pengurus berhasil dikirim! Menunggu verifikasi dan persetujuan Administrator."
                : chosenProgram == ProgramType.TAHFIDZ
                ? "Pendaftaran Tahfidz beserta rekaman tes 1–3 ayat berhasil dikirim! Menunggu verifikasi dan penetapan role oleh Admin/Pengurus."
                : "Pendaftaran berhasil dikirim! Menunggu persetujuan dan penetapan role oleh Admin/Pengurus.";

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", msg, "user", ApiDtos.UserView.of(u)));
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
        if (u.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role akun Anda belum ditetapkan oleh Admin/Pengurus");
        }
        return new ApiDtos.AuthResponse(jwt.create(u), ApiDtos.UserView.of(u));
    }

    @GetMapping("/me")
    public ApiDtos.UserView me(jakarta.servlet.http.HttpServletRequest req) {
        return ApiDtos.UserView.of(CurrentUser.get(req));
    }

    // Req 15: Forgot password via Email (Gmail SMTP)
    @PostMapping("/forgot-password")
    public Map<String, Object> forgotPassword(@Valid @RequestBody ApiDtos.ForgotPasswordRequest r) {
        var u = users.findByEmail(r.email().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Email tidak terdaftar dalam sistem"));

        String token = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant expiry = Instant.now().plus(30, ChronoUnit.MINUTES);
        u.setResetToken(token, expiry);
        users.save(u);

        mailService.sendResetPasswordEmail(u.getEmail(), u.getName(), token, 30);

        return Map.of(
                "message", "Kode reset kata sandi telah dikirimkan ke email " + u.getEmail() + ". Silakan cek kotak masuk atau folder Spam Anda.",
                "email", u.getEmail(),
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
