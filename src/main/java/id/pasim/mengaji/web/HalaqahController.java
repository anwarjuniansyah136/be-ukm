package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.HafalanTaskRepository;
import id.pasim.mengaji.repo.HalaqahAttendanceRepository;
import id.pasim.mengaji.repo.HalaqahSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/halaqah")
public class HalaqahController {
    private final HalaqahSessionRepository sessions;
    private final HalaqahAttendanceRepository attendances;

    public HalaqahController(HalaqahSessionRepository sessions, HalaqahAttendanceRepository attendances) {
        this.sessions = sessions;
        this.attendances = attendances;
    }

    // List all sessions with user-specific attendance indicator
    @GetMapping("/sessions")
    public List<ApiDtos.HalaqahSessionView> listSessions(HttpServletRequest req) {
        UserAccount actor = CurrentUser.get(req);
        var allSessions = sessions.findAllByOrderByScheduledDateDescCreatedAtDesc();
        return allSessions.stream().map(s -> {
            var attOpt = attendances.findBySession_IdAndUser_Id(s.getId(), actor.getId());
            boolean attended = attOpt.map(HalaqahAttendance::isCompleted).orElse(false);
            int watched = attOpt.map(HalaqahAttendance::getWatchedSeconds).orElse(0);
            var attendedAt = attOpt.map(HalaqahAttendance::getAttendedAt).orElse(null);
            long totalAttendees = attendances.countBySession_IdAndCompletedTrue(s.getId());
            return new ApiDtos.HalaqahSessionView(
                    s.getId(),
                    s.getTitle(),
                    s.getDescription(),
                    s.getVideoUrl(),
                    s.getDurationSeconds(),
                    s.getSpeaker(),
                    s.getScheduledDate(),
                    s.getCreatedAt(),
                    attended,
                    watched,
                    attendedAt,
                    totalAttendees
            );
        }).toList();
    }

    // Pengurus posts video pembelajaran
    @PostMapping("/sessions")
    public ApiDtos.HalaqahSessionView createSession(@Valid @RequestBody ApiDtos.HalaqahSessionRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS, Role.ADMIN);
        var session = new HalaqahSession(
                req.title().trim(),
                req.description(),
                req.videoUrl().trim(),
                req.durationSeconds(),
                req.speaker(),
                req.scheduledDate() != null ? req.scheduledDate() : LocalDate.now(),
                actor
        );
        var saved = sessions.save(session);
        return new ApiDtos.HalaqahSessionView(
                saved.getId(),
                saved.getTitle(),
                saved.getDescription(),
                saved.getVideoUrl(),
                saved.getDurationSeconds(),
                saved.getSpeaker(),
                saved.getScheduledDate(),
                saved.getCreatedAt(),
                false,
                0,
                null,
                0
        );
    }

    // Delete session
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<?> deleteSession(@PathVariable UUID id, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS, Role.ADMIN);
        var s = sessions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        sessions.delete(s);
        return ResponseEntity.noContent().build();
    }

    // Anggota reports watch completion / progress (unskippable video attendance)
    @PostMapping("/sessions/{id}/attend")
    public ApiDtos.HalaqahAttendanceView recordAttendance(@PathVariable UUID id, @Valid @RequestBody ApiDtos.HalaqahAttendRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.ANGGOTA);
        var session = sessions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        var existing = attendances.findBySession_IdAndUser_Id(session.getId(), actor.getId()).orElse(null);
        if (existing == null) {
            existing = new HalaqahAttendance(session, actor, req.watchedSeconds(), req.completed(), req.notes());
        } else {
            existing.updateProgress(req.watchedSeconds(), req.completed(), req.notes());
        }
        return ApiDtos.HalaqahAttendanceView.of(attendances.save(existing));
    }

    // Pengurus/Admin views all attendance records for a session
    @GetMapping("/sessions/{id}/attendances")
    public List<ApiDtos.HalaqahAttendanceView> getSessionAttendances(@PathVariable UUID id, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS, Role.ADMIN);
        return attendances.findBySession_IdOrderByCreatedAtAsc(id).stream()
                .map(ApiDtos.HalaqahAttendanceView::of)
                .toList();
    }

    // User gets personal history
    @GetMapping("/my-history")
    public List<ApiDtos.HalaqahAttendanceView> myHistory(HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        return attendances.findByUser_IdOrderByCreatedAtDesc(actor.getId()).stream()
                .map(ApiDtos.HalaqahAttendanceView::of)
                .toList();
    }
}
