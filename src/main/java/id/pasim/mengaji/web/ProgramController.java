package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.ProgramRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/programs")
public class ProgramController {
    private final ProgramRepository programs;

    public ProgramController(ProgramRepository programs) {
        this.programs = programs;
    }

    @GetMapping
    public List<Program> list(HttpServletRequest req) {
        CurrentUser.get(req);
        return programs.findAllByOrderByCreatedAtDesc();
    }

    private void fill(Program p, ApiDtos.ProgramRequest r) {
        p.update(r.name(), r.type(), r.description(), r.scheduledAt(), r.location(), r.groupName(), r.active());
    }

    @PostMapping
    public Program create(@Valid @RequestBody ApiDtos.ProgramRequest r, HttpServletRequest req) {
        CurrentUser.require(CurrentUser.get(req), Role.ADMIN, Role.PENGURUS);
        var p = new Program(r.name(), r.type(), r.description());
        fill(p, r);
        return programs.save(p);
    }

    @PutMapping("/{id}")
    @Transactional
    public Program update(@PathVariable UUID id, @Valid @RequestBody ApiDtos.ProgramRequest r, HttpServletRequest req) {
        CurrentUser.require(CurrentUser.get(req), Role.ADMIN, Role.PENGURUS);
        var p = programs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getType() != r.type()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Jenis program tidak dapat diganti");
        }
        fill(p, r);
        return programs.save(p);
    }

    @PostMapping("/{id}/join")
    @Transactional
    public Program join(@PathVariable UUID id, HttpServletRequest req) {
        var a = CurrentUser.get(req);
        CurrentUser.require(a, Role.ANGGOTA);
        var p = programs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!p.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program tidak aktif");
        }

        // Req 3: Halaqah diikuti seluruh anggota tanpa batasan; program utama (Tahlil/Tahfidz) hanya boleh 1 hingga selesai
        if (p.getType() != ProgramType.HALAQAH) {
            MemberStatus st = a.getMemberStatus();
            if (p.getType() == ProgramType.TAHFIDZ && st == MemberStatus.AKTIF_TAHLIL) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Anggota hanya boleh mengikuti 1 program utama. Selesaikan program Tahlil terlebih dahulu sebelum mengikuti Tahfidz."
                );
            }
            if (p.getType() == ProgramType.TAHLIL && (
                    st == MemberStatus.MENUNGGU_TES_TAHFIDZ
                    || st == MemberStatus.MEMILIH_TARGET_TAHFIDZ
                    || st == MemberStatus.MENUNGGU_APPROVAL_DURASI
                    || st == MemberStatus.AKTIF_TAHFIDZ
            )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Anda sedang mengikuti program Tahfidz. Selesaikan program Tahfidz terlebih dahulu sebelum mengambil program lain."
                );
            }
            boolean joinedOtherMainProgram = programs.findAllByOrderByCreatedAtDesc().stream()
                    .anyMatch(other -> !other.getId().equals(p.getId())
                            && other.getType() != ProgramType.HALAQAH
                            && other.getType() != p.getType()
                            && other.getParticipants().contains(a.getId()));
            if (joinedOtherMainProgram && st != MemberStatus.LULUS_TAHLIL && st != MemberStatus.LULUS_TAHFIDZ) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Anggota hanya diperbolehkan mengikuti 1 program utama hingga selesai (kecuali Halaqah)."
                );
            }
        }

        p.join(a.getId());
        return programs.save(p);
    }

    @DeleteMapping("/{id}/join")
    @Transactional
    public Program leave(@PathVariable UUID id, HttpServletRequest req) {
        var a = CurrentUser.get(req);
        CurrentUser.require(a, Role.ANGGOTA);
        var p = programs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        p.leave(a.getId());
        return programs.save(p);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable UUID id, HttpServletRequest req) {
        CurrentUser.require(CurrentUser.get(req), Role.ADMIN, Role.PENGURUS);
        var p = programs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        programs.delete(p);
        return ResponseEntity.noContent().build();
    }
}
