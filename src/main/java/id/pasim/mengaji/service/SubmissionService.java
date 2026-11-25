package id.pasim.mengaji.service;

import id.pasim.mengaji.web.ApiDtos;
import id.pasim.mengaji.web.CurrentUser;
import org.springframework.stereotype.Service;

import id.pasim.mengaji.domain.*;
import id.pasim.mengaji.repo.HafalanTaskRepository;
import id.pasim.mengaji.repo.ProgramRepository;
import id.pasim.mengaji.repo.SubmissionRepository;
import id.pasim.mengaji.repo.TahlilTaskRepository;
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
    private final TahlilTaskRepository tahlilTasks;
    private final ProgramRepository programs;

    public SubmissionService(
            SubmissionRepository submissions,
            HafalanTaskRepository tasks,
            TahlilTaskRepository tahlilTasks,
            ProgramRepository programs
    ) {
        this.submissions = submissions;
        this.tasks = tasks;
        this.tahlilTasks = tahlilTasks;
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
            if (actor.getMemberStatus() == MemberStatus.AKTIF_TAHLIL) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Selesaikan program Tahlil terlebih dahulu sebelum mengirim setoran Tahfidz");
            }
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

        // Req 3: Anggota yang sedang aktif di Tahfidz tidak boleh menyetor Tahlil (hanya 1 program utama sampai selesai)
        MemberStatus st = actor.getMemberStatus();
        if (st == MemberStatus.MENUNGGU_TES_TAHFIDZ
                || st == MemberStatus.MEMILIH_TARGET_TAHFIDZ
                || st == MemberStatus.MENUNGGU_APPROVAL_DURASI
                || st == MemberStatus.AKTIF_TAHFIDZ) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Anda sedang aktif di program Tahfidz. Anggota hanya boleh mengikuti 1 program utama hingga selesai (kecuali Halaqah)."
            );
        }

        // Tahlil Submission (Video and/or Image) - harus berdasarkan tugas setoran dari Pengurus
        if (request.tahlilTaskId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Pilih tugas setoran Tahlil dari Pengurus terlebih dahulu sebelum mengirim setoran."
            );
        }
        var tahlilTask = tahlilTasks.findById(request.tahlilTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tugas setoran Tahlil tidak ditemukan"));
        if (tahlilTask.getMember() != null && !tahlilTask.getMemberId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tugas setoran Tahlil ini bukan untuk akun Anda");
        }
        var latestTahlil = submissions.findFirstByTahlilTask_IdAndMember_IdOrderBySubmittedAtDesc(tahlilTask.getId(), actor.getId());
        if (latestTahlil.isPresent() && latestTahlil.get().getStatus() != SubmissionStatus.PERLU_PERBAIKAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Anda sudah mengirim setoran untuk tugas ini; kirim ulang hanya jika status perlu perbaikan");
        }

        boolean hasVideo = request.recordingUrl() != null && !request.recordingUrl().isBlank();
        boolean hasImage = request.imageUrl() != null && !request.imageUrl().isBlank();
        if (!hasVideo && !hasImage) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Kirimkan setoran video dan/atau bukti gambar bacaan Tahlil");
        }

        Program program = tahlilTask.getProgram();
        if (program == null && request.programId() != null) {
            program = programs.findById(request.programId()).orElse(null);
        }
        if (program == null) {
            program = programs.findByTypeAndActiveTrue(ProgramType.TAHLIL).stream().findFirst().orElse(null);
        }

        var sub = new Submission(null, program, ProgramType.TAHLIL, actor, request.note(), request.recordingUrl(), request.imageUrl());
        sub.setTahlilTask(tahlilTask);
        return ApiDtos.SubmissionView.of(submissions.save(sub));
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
