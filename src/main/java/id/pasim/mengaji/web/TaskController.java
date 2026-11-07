package id.pasim.mengaji.web;

import id.pasim.mengaji.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }

    @GetMapping
    public List<ApiDtos.TaskView> list(HttpServletRequest req) {
        return service.list(CurrentUser.get(req));
    }

    @PostMapping
    public ApiDtos.TaskView create(@Valid @RequestBody ApiDtos.TaskRequest r, HttpServletRequest req) {
        return service.create(r, CurrentUser.get(req));
    }

    @PatchMapping("/{id}/duration")
    public ApiDtos.TaskView choose(@PathVariable UUID id, @Valid @RequestBody ApiDtos.DurationRequest r, HttpServletRequest req) {
        return service.choose(id, r, CurrentUser.get(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id, HttpServletRequest req) {
        return service.delete(id, CurrentUser.get(req));
    }
}
