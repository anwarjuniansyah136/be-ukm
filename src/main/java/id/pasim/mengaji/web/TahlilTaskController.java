package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.*;
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
@RequestMapping("/api/tahlil-tasks")
public class TahlilTaskController {
    private final TahlilTaskRepository tahlilTasks;
    private final ProgramRepository programs;
    private final UserRepository users;

    public TahlilTaskController(TahlilTaskRepository tahlilTasks, ProgramRepository programs, UserRepository users) {
        this.tahlilTasks = tahlilTasks;
        this.programs = programs;
        this.users = users;
    }

    @GetMapping
    public List<ApiDtos.TahlilTaskView> list(HttpServletRequest req) {
        var actor = CurrentUser.get(req);
        List<TahlilTask> rows = (actor.getRole() == Role.ADMIN || actor.getRole() == Role.PENGURUS)
                ? tahlilTasks.findAllByOrderByCreatedAtDesc()
                : tahlilTasks.findByMemberIsNullOrMember_IdOrderByCreatedAtDesc(actor.getId());
        return rows.stream().map(ApiDtos.TahlilTaskView::of).toList();
    }

    @PostMapping
    @Transactional
    public ApiDtos.TahlilTaskView create(@Valid @RequestBody ApiDtos.TahlilTaskRequest req, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS);

        Program program = null;
        if (req.programId() != null) {
            program = programs.findById(req.programId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program Tahlil tidak ditemukan"));
        } else {
            program = programs.findByTypeAndActiveTrue(ProgramType.TAHLIL).stream().findFirst().orElse(null);
            if (program == null) {
                program = programs.findAllByOrderByCreatedAtDesc().stream()
                        .filter(p -> p.getType() == ProgramType.TAHLIL)
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Program Tahlil belum tersedia"));
            }
        }

        UserAccount targetMember = null;
        if (req.memberId() != null) {
            targetMember = users.findById(req.memberId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anggota tidak ditemukan"));
        }

        var task = new TahlilTask(
                program,
                targetMember,
                req.title().trim(),
                req.instruction() != null ? req.instruction().trim() : "",
                req.dueDate(),
                actor
        );
        return ApiDtos.TahlilTaskView.of(tahlilTasks.save(task));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable UUID id, HttpServletRequest httpReq) {
        var actor = CurrentUser.get(httpReq);
        CurrentUser.require(actor, Role.PENGURUS);
        var task = tahlilTasks.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        tahlilTasks.delete(task);
        return ResponseEntity.noContent().build();
    }
}
