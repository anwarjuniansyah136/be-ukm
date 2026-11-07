package id.pasim.mengaji.service;

import id.pasim.mengaji.web.ApiDtos;
import id.pasim.mengaji.web.CurrentUser;
import org.springframework.stereotype.Service;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional

public class TaskService {
    private final HafalanTaskRepository tasks;
    private final ProgramRepository programs;
    private final UserRepository users;

    public TaskService(HafalanTaskRepository t, ProgramRepository p, UserRepository u) {
        tasks = t;
        programs = p;
        users = u;
    }

    public List<ApiDtos.TaskView> list(UserAccount currentActor) {
        var a = currentActor;
        return (a.getRole() == Role.ANGGOTA ? tasks.findByMember_IdOrderByDueDateAsc(a.getId()) : tasks.findAllByOrderByDueDateAsc())
                .stream().map(ApiDtos.TaskView::of).toList();
    }

    public ApiDtos.TaskView create(ApiDtos.TaskRequest r, UserAccount currentActor) {
        var actor = currentActor;
        CurrentUser.require(actor, Role.PENGURUS);
        if (r.ayatEnd() < r.ayatStart()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rentang ayat tidak valid");
        }
        var p = programs.findById(r.programId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var m = users.findById(r.memberId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (p.getType() != ProgramType.TAHFIDZ || !p.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pilih program Tahfidz aktif");
        }
        if (m.getRole() != Role.ANGGOTA || m.getStatus() != RegistrationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pilih anggota yang disetujui");
        }
        if (r.allowedDurations().stream().anyMatch(days -> !List.of(7, 14, 21, 30).contains(days))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Durasi harus 1, 2, 3 minggu atau maksimal 1 bulan");
        }
        String options = r.allowedDurations().stream().distinct().sorted().map(String::valueOf).collect(Collectors.joining(","));
        return ApiDtos.TaskView.of(tasks.save(new HafalanTask(p, m, r.surah(), r.ayatStart(), r.ayatEnd(), options, r.startDate(), r.note(), actor)));
    }

    @Transactional
    public ApiDtos.TaskView choose(UUID id, ApiDtos.DurationRequest r, UserAccount currentActor) {
        var actor = currentActor;
        CurrentUser.require(actor, Role.ANGGOTA);
        var task = tasks.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!task.getMemberId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (task.getDurationDays() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Durasi sudah dikonfirmasi dan tidak dapat diubah");
        }
        if (!task.getAllowedDurations().contains(r.durationDays())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Durasi tidak tersedia untuk tugas ini");
        }
        task.chooseDuration(r.durationDays());
        return ApiDtos.TaskView.of(tasks.save(task));
    }

    @Transactional
    public ResponseEntity<?> delete(UUID id, UserAccount currentActor) {
        CurrentUser.require(currentActor, Role.PENGURUS);
        var task = tasks.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tasks.delete(task);
        return ResponseEntity.noContent().build();
    }
}
