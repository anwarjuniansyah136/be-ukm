package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.TahfidzAuditionRepository;
import id.pasim.mengaji.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TahfidzAuditionRepository auditions;

    public AdminController(UserRepository users, PasswordEncoder passwordEncoder, TahfidzAuditionRepository auditions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.auditions = auditions;
    }

    @GetMapping("/users")
    public List<ApiDtos.UserView> users(HttpServletRequest req) {
        CurrentUser.require(CurrentUser.get(req), Role.ADMIN, Role.PENGURUS);
        return users.findAllByOrderByCreatedAtDesc().stream().map(ApiDtos.UserView::of).toList();
    }

    @GetMapping("/registrations")
    public List<ApiDtos.UserView> pending(HttpServletRequest req) {
        CurrentUser.require(CurrentUser.get(req), Role.ADMIN, Role.PENGURUS);
        return users.findByStatusOrderByCreatedAtDesc(RegistrationStatus.PENDING).stream().map(ApiDtos.UserView::of).toList();
    }

    @PatchMapping("/registrations/{id}")
    @Transactional
    public ApiDtos.UserView decide(@PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN, Role.PENGURUS);
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getStatus() != RegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pendaftaran sudah diproses");
        }
        if (actor.getRole() == Role.PENGURUS && user.getRequestedRole() == Role.PENGURUS) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Admin yang berwenang menyetujui calon Pengurus");
        }
        String statusStr = body.getOrDefault("status", "").toUpperCase();
        if (!Set.of("APPROVED", "REJECTED").contains(statusStr)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status tidak valid");
        }
        boolean approve = "APPROVED".equals(statusStr);

        if (!approve) {
            // Ditolak: role tetap null (belum ditetapkan)
            user.decide(null, RegistrationStatus.REJECTED);
            auditions.findFirstByMember_IdOrderByCreatedAtDesc(user.getId()).ifPresent(a -> {
                if (a.getStatus() == AuditionStatus.MENUNGGU_REVIEW) {
                    a.review(actor, AuditionStatus.DITOLAK, body.getOrDefault("reviewNote", "Pendaftaran ditolak"));
                    auditions.save(a);
                }
            });
            return ApiDtos.UserView.of(users.save(user));
        }

        // Penetapan role dilakukan saat approval (sebelumnya role null / belum jelas)
        Role assignedRole = user.getRequestedRole() != null ? user.getRequestedRole() : Role.ANGGOTA;
        if (body.containsKey("role") && body.get("role") != null && !body.get("role").isBlank()) {
            try {
                Role requestedAssign = Role.valueOf(body.get("role").toUpperCase());
                if (actor.getRole() == Role.PENGURUS && requestedAssign != Role.ANGGOTA) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pengurus hanya dapat menetapkan role Anggota");
                }
                assignedRole = requestedAssign;
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role tidak valid");
            }
        }

        user.decide(assignedRole, RegistrationStatus.APPROVED);

        // Jika disetujui sebagai ANGGOTA dan mendaftar jalur Tahfidz (sudah ikut tes 1-3 ayat saat pendaftaran awal)
        if (assignedRole == Role.ANGGOTA && user.getRequestedProgram() == ProgramType.TAHFIDZ) {
            String tahfidzDecision = body.getOrDefault("tahfidzDecision", "APPROVED_TAHFIDZ").toUpperCase();
            boolean passTahfidz = !"DIRECT_TO_TAHLIL".equals(tahfidzDecision);
            String note = body.getOrDefault("reviewNote",
                    passTahfidz ? "Tes bacaan 1–3 ayat pendaftaran awal disetujui. Silakan pilih target hafalan."
                            : "Disarankan mengikuti program Tahlil terlebih dahulu.");

            auditions.findFirstByMember_IdOrderByCreatedAtDesc(user.getId()).ifPresent(a -> {
                a.review(actor, passTahfidz ? AuditionStatus.DISETUJUI : AuditionStatus.DITOLAK, note);
                auditions.save(a);
            });
            user.updateMemberStatus(passTahfidz ? MemberStatus.MEMILIH_TARGET_TAHFIDZ : MemberStatus.AKTIF_TAHLIL);
        } else if (assignedRole == Role.ANGGOTA) {
            user.updateMemberStatus(MemberStatus.AKTIF_TAHLIL);
        }

        return ApiDtos.UserView.of(users.save(user));
    }

    @PatchMapping("/users/{id}/role")
    public ApiDtos.UserView role(@PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN);
        if (actor.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tidak dapat mengubah peran sendiri");
        }
        Role role;
        try {
            role = Role.valueOf(body.get("role").toUpperCase());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role tidak valid");
        }
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getStatus() != RegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Akun belum disetujui");
        }
        user.decide(role, user.getStatus());
        return ApiDtos.UserView.of(users.save(user));
    }

    // Graduate member from Tahlil -> Directed to Tahfidz
    @PatchMapping("/members/{id}/graduate-tahlil")
    public ApiDtos.UserView graduateTahlil(@PathVariable UUID id, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN, Role.PENGURUS);
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.updateMemberStatus(MemberStatus.LULUS_TAHLIL);
        return ApiDtos.UserView.of(users.save(user));
    }

    // Graduate member from Tahfidz -> Selesai / Alumni
    @PatchMapping("/members/{id}/graduate-tahfidz")
    public ApiDtos.UserView graduateTahfidz(@PathVariable UUID id, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN, Role.PENGURUS);
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.updateMemberStatus(MemberStatus.LULUS_TAHFIDZ);
        return ApiDtos.UserView.of(users.save(user));
    }

    // Update member status
    @PatchMapping("/members/{id}/status")
    public ApiDtos.UserView updateMemberStatus(@PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN, Role.PENGURUS);
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            MemberStatus status = MemberStatus.valueOf(body.get("memberStatus").toUpperCase());
            user.updateMemberStatus(status);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status anggota tidak valid");
        }
        return ApiDtos.UserView.of(users.save(user));
    }

    @PostMapping("/users/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN);
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String newPassword = body.getOrDefault("newPassword", "Pasim12345678!");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        return Map.of("message", "Kata sandi akun " + user.getEmail() + " berhasil direset");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN);
        if (actor.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tidak dapat menghapus akun sendiri");
        }
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        users.delete(user);
        return ResponseEntity.noContent().build();
    }
}
