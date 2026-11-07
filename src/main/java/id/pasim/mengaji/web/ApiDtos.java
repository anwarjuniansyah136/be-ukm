package id.pasim.mengaji.web;

import id.pasim.mengaji.domain.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public class ApiDtos {
    public record RegisterRequest(
            @NotBlank @Size(max=180) String name,
            @Email @NotBlank @Size(max=255) String email,
            @NotBlank @Size(min=8,max=72) String password,
            @NotNull Role requestedRole
    ) {}

    public record LoginRequest(
            @Email @NotBlank @Size(max=255) String email,
            @NotBlank String password
    ) {}

    public record UserView(
            UUID id,
            String name,
            String email,
            Role role,
            Role requestedRole,
            RegistrationStatus status,
            MemberStatus memberStatus,
            String phone,
            String nim,
            String prodi,
            String bio,
            String avatarUrl,
            Instant createdAt
    ) {
        public static UserView of(UserAccount u) {
            return new UserView(
                    u.getId(),
                    u.getName(),
                    u.getEmail(),
                    u.getRole(),
                    u.getRequestedRole(),
                    u.getStatus(),
                    u.getMemberStatus(),
                    u.getPhone(),
                    u.getNim(),
                    u.getProdi(),
                    u.getBio(),
                    u.getAvatarUrl(),
                    u.getCreatedAt()
            );
        }
    }

    public record ProfileUpdateRequest(
            @NotBlank @Size(max=180) String name,
            @Size(max=30) String phone,
            @Size(max=30) String nim,
            @Size(max=100) String prodi,
            @Size(max=2000) String bio,
            @Size(max=1000) String avatarUrl
    ) {}

    public record ForgotPasswordRequest(
            @Email @NotBlank String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min=8, max=72) String newPassword
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min=8, max=72) String newPassword
    ) {}

    public record AuthResponse(String token, UserView user) {}

    public record ProgramRequest(
            @NotBlank @Size(max=180) String name,
            @NotNull ProgramType type,
            @Size(max=2000) String description,
            OffsetDateTime scheduledAt,
            @Size(max=255) String location,
            @Size(max=255) String groupName,
            boolean active
    ) {}

    public record MaterialRequest(
            @NotNull UUID programId,
            @NotBlank @Size(max=255) String title,
            @Size(max=4000) String body,
            @Size(max=1000) @Pattern(regexp="^$|https?://[^\\s]+", message="Gunakan URL http(s)") String resourceUrl
    ) {}

    public record TaskRequest(
            @NotNull UUID programId,
            @NotNull UUID memberId,
            @NotBlank @Size(max=180) String surah,
            @Min(1) int ayatStart,
            @Min(1) int ayatEnd,
            @NotEmpty @Size(max=12) java.util.List<@NotNull @Min(1) @Max(365) Integer> allowedDurations,
            @NotNull LocalDate startDate,
            @Size(max=1000) String note
    ) {}

    public record TaskView(
            UUID id,
            UUID programId,
            UUID memberId,
            String surah,
            int ayatStart,
            int ayatEnd,
            Integer durationDays,
            java.util.List<Integer> allowedDurations,
            LocalDate startDate,
            LocalDate dueDate,
            String note,
            Instant createdAt
    ) {
        public static TaskView of(HafalanTask t) {
            return new TaskView(
                    t.getId(),
                    t.getProgramId(),
                    t.getMemberId(),
                    t.getSurah(),
                    t.getAyatStart(),
                    t.getAyatEnd(),
                    t.getDurationDays(),
                    t.getAllowedDurations(),
                    t.getStartDate(),
                    t.getDueDate(),
                    t.getNote(),
                    t.getCreatedAt()
            );
        }
    }

    public record SubmissionRequest(
            UUID taskId,
            UUID programId,
            ProgramType programType,
            @Size(max=3000) String note,
            @Size(max=1000) String recordingUrl,
            @Size(max=1000) String imageUrl
    ) {}

    public record ReviewRequest(
            @NotNull SubmissionStatus status,
            @Size(max=2000) String reviewNote
    ) {}

    public record SubmissionView(
            UUID id,
            UUID taskId,
            UUID programId,
            ProgramType programType,
            String programName,
            UUID memberId,
            String memberName,
            String note,
            String recordingUrl,
            String imageUrl,
            SubmissionStatus status,
            String reviewNote,
            String reviewedByName,
            Instant submittedAt,
            Instant reviewedAt
    ) {
        public static SubmissionView of(Submission s) {
            String progName = "";
            if (s.getTask() != null) {
                progName = "Tahfidz - " + s.getTask().getSurah() + " (" + s.getTask().getAyatStart() + "-" + s.getTask().getAyatEnd() + ")";
            } else if (s.getProgram() != null) {
                progName = s.getProgram().getName();
            } else if (s.getProgramType() == ProgramType.TAHLIL) {
                progName = "Program Tahlil";
            }
            return new SubmissionView(
                    s.getId(),
                    s.getTaskId(),
                    s.getProgramId(),
                    s.getProgramType(),
                    progName,
                    s.getMemberId(),
                    s.getMember() != null ? s.getMember().getName() : null,
                    s.getNote(),
                    s.getRecordingUrl(),
                    s.getImageUrl(),
                    s.getStatus(),
                    s.getReviewNote(),
                    s.getReviewedBy() != null ? s.getReviewedBy().getName() : null,
                    s.getSubmittedAt(),
                    s.getReviewedAt()
            );
        }
    }

    public record DurationRequest(@Min(1) @Max(365) int durationDays) {}

    // Activities / Kegiatan DTOs
    public record ActivityRequest(
            @NotBlank @Size(max=200) String title,
            @Size(max=3000) String description,
            @NotNull LocalDate eventDate,
            @Size(max=50) String eventTime,
            @Size(max=255) String location,
            @Size(max=200) String speaker,
            ActivityStatus status
    ) {}

    public record ActivityView(
            UUID id,
            String title,
            String description,
            LocalDate eventDate,
            String eventTime,
            String location,
            String speaker,
            ActivityStatus status,
            String createdByName,
            Instant createdAt
    ) {
        public static ActivityView of(Activity a) {
            return new ActivityView(
                    a.getId(),
                    a.getTitle(),
                    a.getDescription(),
                    a.getEventDate(),
                    a.getEventTime(),
                    a.getLocation(),
                    a.getSpeaker(),
                    a.getStatus(),
                    a.getCreatedBy() != null ? a.getCreatedBy().getName() : "Admin UKM",
                    a.getCreatedAt()
            );
        }
    }

    // Halaqah DTOs
    public record HalaqahSessionRequest(
            @NotBlank @Size(max=200) String title,
            @Size(max=3000) String description,
            @NotBlank @Size(max=1000) String videoUrl,
            @Min(10) int durationSeconds,
            @Size(max=200) String speaker,
            LocalDate scheduledDate
    ) {}

    public record HalaqahAttendRequest(
            @Min(0) int watchedSeconds,
            boolean completed,
            @Size(max=1000) String notes
    ) {}

    public record HalaqahSessionView(
            UUID id,
            String title,
            String description,
            String videoUrl,
            int durationSeconds,
            String speaker,
            LocalDate scheduledDate,
            Instant createdAt,
            boolean attended,
            int watchedSeconds,
            Instant attendedAt,
            long totalAttendees
    ) {}

    public record HalaqahAttendanceView(
            UUID id,
            UUID sessionId,
            UUID userId,
            String userName,
            String userEmail,
            int watchedSeconds,
            boolean completed,
            Instant attendedAt,
            String notes
    ) {
        public static HalaqahAttendanceView of(HalaqahAttendance a) {
            return new HalaqahAttendanceView(
                    a.getId(),
                    a.getSession().getId(),
                    a.getUser().getId(),
                    a.getUser().getName(),
                    a.getUser().getEmail(),
                    a.getWatchedSeconds(),
                    a.isCompleted(),
                    a.getAttendedAt(),
                    a.getNotes()
            );
        }
    }

    // Tahfidz Audition / Target DTOs
    public record RandomAyatView(
            String surah,
            int ayatStart,
            int ayatEnd,
            String arabic,
            String latin,
            String translation,
            String instructions
    ) {}

    public record TahfidzTestSubmitRequest(
            @NotBlank String surah,
            @Min(1) int ayatStart,
            @Min(1) int ayatEnd,
            @NotBlank String recordingUrl,
            @Size(max=2000) String memberNote
    ) {}

    public record AuditionReviewRequest(
            @NotNull AuditionStatus status,
            @Size(max=2000) String reviewNote
    ) {}

    public record TahfidzTargetSubmitRequest(
            @NotBlank String targetSurah,
            @Min(1) int targetAyatStart,
            @Min(1) int targetAyatEnd,
            @NotNull Integer chosenDurationDays
    ) {}

    public record TahfidzAuditionView(
            UUID id,
            UUID memberId,
            String memberName,
            String memberEmail,
            String randomSurah,
            int randomAyatStart,
            int randomAyatEnd,
            String recordingUrl,
            String memberNote,
            AuditionStatus status,
            String reviewNote,
            String reviewedByName,
            Instant reviewedAt,
            String targetSurah,
            Integer targetAyatStart,
            Integer targetAyatEnd,
            Integer chosenDurationDays,
            AuditionStatus targetApprovalStatus,
            String targetReviewNote,
            String targetReviewedByName,
            Instant targetReviewedAt,
            Instant createdAt
    ) {
        public static TahfidzAuditionView of(TahfidzAudition a) {
            return new TahfidzAuditionView(
                    a.getId(),
                    a.getMember().getId(),
                    a.getMember().getName(),
                    a.getMember().getEmail(),
                    a.getRandomSurah(),
                    a.getRandomAyatStart(),
                    a.getRandomAyatEnd(),
                    a.getRecordingUrl(),
                    a.getMemberNote(),
                    a.getStatus(),
                    a.getReviewNote(),
                    a.getReviewedBy() != null ? a.getReviewedBy().getName() : null,
                    a.getReviewedAt(),
                    a.getTargetSurah(),
                    a.getTargetAyatStart(),
                    a.getTargetAyatEnd(),
                    a.getChosenDurationDays(),
                    a.getTargetApprovalStatus(),
                    a.getTargetReviewNote(),
                    a.getTargetReviewedBy() != null ? a.getTargetReviewedBy().getName() : null,
                    a.getTargetReviewedAt(),
                    a.getCreatedAt()
            );
        }
    }

    private ApiDtos() {}
}
