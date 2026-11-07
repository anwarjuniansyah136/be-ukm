package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.Activity;
import id.pasim.mengaji.domain.ActivityStatus;
import id.pasim.mengaji.domain.Role;
import id.pasim.mengaji.repo.ActivityRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityRepository activities;

    public ActivityController(ActivityRepository activities) {
        this.activities = activities;
    }

    // Public / all authenticated users can list activities
    @GetMapping
    public List<ApiDtos.ActivityView> list() {
        return activities.findAllByOrderByEventDateAsc().stream().map(ApiDtos.ActivityView::of).toList();
    }

    // Admin creates activity (e.g. 16 Desember Pengajian Bersama)
    @PostMapping
    public ApiDtos.ActivityView create(@Valid @RequestBody ApiDtos.ActivityRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.ADMIN);
        var act = new Activity(
                req.title().trim(),
                req.description(),
                req.eventDate(),
                req.eventTime(),
                req.location(),
                req.speaker(),
                req.status() != null ? req.status() : ActivityStatus.DIJADWALKAN,
                actor
        );
        return ApiDtos.ActivityView.of(activities.save(act));
    }

    // Admin updates activity
    @PutMapping("/{id}")
    public ApiDtos.ActivityView update(@PathVariable UUID id, @Valid @RequestBody ApiDtos.ActivityRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.ADMIN);
        var act = activities.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        act.update(
                req.title().trim(),
                req.description(),
                req.eventDate(),
                req.eventTime(),
                req.location(),
                req.speaker(),
                req.status()
        );
        return ApiDtos.ActivityView.of(activities.save(act));
    }

    // Admin updates status ("apakah ini jalan atau tidak": DIJADWALKAN, SEDANG_BERJALAN, SELESAI, DIBATALKAN)
    @PatchMapping("/{id}/status")
    public ApiDtos.ActivityView updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.ADMIN);
        var act = activities.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        try {
            ActivityStatus status = ActivityStatus.valueOf(body.get("status").toUpperCase());
            act.updateStatus(status);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status kegiatan tidak valid");
        }
        return ApiDtos.ActivityView.of(activities.save(act));
    }

    // Admin deletes activity
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.ADMIN);
        var act = activities.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        activities.delete(act);
        return ResponseEntity.noContent().build();
    }
}
