package id.pasim.mengaji.web;

import id.pasim.mengaji.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionService service;
    public SubmissionController(SubmissionService service) { this.service = service; }

    @GetMapping
    public List<ApiDtos.SubmissionView> list(HttpServletRequest req) {
        return service.list(CurrentUser.get(req));
    }

    @PostMapping
    public ApiDtos.SubmissionView submit(@Valid @RequestBody ApiDtos.SubmissionRequest request, HttpServletRequest req) {
        return service.submit(request, CurrentUser.get(req));
    }

    @PatchMapping("/{id}/review")
    public ApiDtos.SubmissionView review(@PathVariable UUID id, @Valid @RequestBody ApiDtos.ReviewRequest request, HttpServletRequest req) {
        return service.review(id, request, CurrentUser.get(req));
    }
}
