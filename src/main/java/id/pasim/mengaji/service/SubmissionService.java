package id.pasim.mengaji.service;

import id.pasim.mengaji.web.ApiDtos;
import id.pasim.mengaji.web.CurrentUser;
import org.springframework.stereotype.Service;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.HafalanTaskRepository;
import id.pasim.mengaji.repo.ProgramRepository;
import id.pasim.mengaji.repo.SubmissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubmissionService {
    private final SubmissionRepository submissions;
    private final HafalanTaskRepository tasks;
    private final ProgramRepository programs;

    public SubmissionService(SubmissionRepository submissions, HafalanTaskRepository tasks, ProgramRepository programs) {
        this.submissions = submissions;
        this.tasks = tasks;
        this.programs = programs;
    }

    public List<ApiDtos.SubmissionView> list(UserAccount currentActor) {
        var actor = currentActor;
        var all = actor.getRole() == Role.ADMIN || actor.getRole() == Role.PENGURUS
                ? submissions.findAllByOrderBySubmittedAtDesc()
                : submissions.findByMember_IdOrderBySubmittedAtDesc(actor.getId());
        return all.stream().map(ApiDtos.SubmissionView::of).toList();
    }

    @Transactional
    public ApiDtos.SubmissionView submit(ApiDtos.SubmissionRequest request, UserAccount currentActor) {
        var actor = currentActor;
        CurrentUser.require(actor, Role.ANGGOTA);

        // Tahfidz Task Submission
        if (request.taskId() != null) {
            var task = tasks.lockById(request.taskId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            if (!task.getMemberId().equals(actor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tugas ini bukan milik akun Anda");
            }
            if (task.getDurationDays() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Konfirmasi durasi sebelum menyetor hafalan");
            }
            if (request.recordingUrl() == null || request.recordingUrl().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tautan rekaman setoran video/audio wajib diisi");
            }
            var latest = submissions.findFirstByTask_IdOrderBySubmittedAtDesc(task.getId());
            if (latest.isPresent() && latest.get().getStatus() != SubmissionStatus.PERLU_PERBAIKAN) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tunggu penilaian; kirim ulang hanya jika status perlu perbaikan");
            }
            return ApiDtos.SubmissionView.of(submissions.save(new Submission(task, null, ProgramType.TAHFIDZ, actor, request.note(), request.recordingUrl(), request.imageUrl())));
        }

        // Tahlil Submission (Video and/or Image)
        if (request.recordingUrl() == null && request.imageUrl() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kirimkan setoran video atau bukti gambar bacaan Tahlil");
        }
        if ((request.recordingUrl() != null && request.recordingUrl().isBlank()) && (request.imageUrl() != null && request.imageUrl().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kirimkan setoran video atau bukti gambar bacaan Tahlil");
        }

        Program program = null;
        if (request.programId() != null) {
            program = programs.findById(request.programId()).orElse(null);
        }
        if (program == null) {
            program = programs.findByTypeAndActiveTrue(ProgramType.TAHLIL).stream().findFirst().orElse(null);
        }

        return ApiDtos.SubmissionView.of(submissions.save(
                new Submission(null, program, ProgramType.TAHLIL, actor, request.note(), request.recordingUrl(), request.imageUrl())
        ));
    }

    @Transactional
    public ApiDtos.SubmissionView review(UUID id, ApiDtos.ReviewRequest request, UserAccount currentActor) {
        var actor = currentActor;
        CurrentUser.require(actor, Role.PENGURUS);
        var submission = submissions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (request.status() == SubmissionStatus.MENUNGGU) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status penilaian tidak valid");
        }
        if (submission.getStatus() != SubmissionStatus.MENUNGGU) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setoran ini sudah dinilai; periksa setoran revisi terbaru");
        }
        submission.review(actor, request.status(), request.reviewNote());
        return ApiDtos.SubmissionView.of(submissions.save(submission));
    }
}
