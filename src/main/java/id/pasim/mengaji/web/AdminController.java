package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
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
    public ApiDtos.UserView decide(@PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        CurrentUser.require(actor, Role.ADMIN, Role.PENGURUS);
        var user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (user.getStatus() != RegistrationStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pendaftaran sudah diproses");
        }
        if (actor.getRole() == Role.PENGURUS && user.getRequestedRole() != Role.ANGGOTA) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Hanya Admin yang berwenang menyetujui calon Pengurus");
        }
        String statusStr = body.getOrDefault("status", "").toUpperCase();
        if (!Set.of("APPROVED", "REJECTED").contains(statusStr)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status tidak valid");
        }
        boolean approve = "APPROVED".equals(statusStr);
        Role assignedRole = user.getRequestedRole();
        if (actor.getRole() == Role.ADMIN && body.containsKey("role") && !body.get("role").isBlank()) {
            try {
                assignedRole = Role.valueOf(body.get("role").toUpperCase());
            } catch (Exception ignored) {}
        }
        user.decide(approve ? assignedRole : Role.ANGGOTA, approve ? RegistrationStatus.APPROVED : RegistrationStatus.REJECTED);
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
