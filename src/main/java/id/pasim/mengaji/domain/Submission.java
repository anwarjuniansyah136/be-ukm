package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="submissions")
public class Submission {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional=true)
    @JoinColumn(name="task_id", nullable=true)
    private HafalanTask task;

    @ManyToOne(optional=true)
    @JoinColumn(name="tahlil_task_id", nullable=true)
    private TahlilTask tahlilTask;

    @ManyToOne
    @JoinColumn(name="program_id")
    private Program program;

    @Enumerated(EnumType.STRING)
    private ProgramType programType;

    @ManyToOne(optional=false)
    @JoinColumn(name="member_id")
    private UserAccount member;

    @Column(length=3000)
    private String note;

    @Column(length=1000)
    private String recordingUrl;

    @Column(length=1000)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private SubmissionStatus status = SubmissionStatus.MENUNGGU;

    @ManyToOne
    @JoinColumn(name="reviewed_by")
    private UserAccount reviewedBy;

    @Column(length=2000)
    private String reviewNote;

    private Instant reviewedAt;

    @Column(nullable=false)
    private Instant submittedAt = Instant.now();

    @Version
    private long version;

    protected Submission() {}

    // Legacy constructor for Tahfidz Task
    public Submission(HafalanTask task, UserAccount member, String note, String url) {
        this.task = task;
        if (task != null) {
            this.programType = ProgramType.TAHFIDZ;
        }
        this.member = member;
        this.note = note;
        this.recordingUrl = url;
        this.submittedAt = Instant.now();
    }

    // Flexible constructor for Tahfidz or Tahlil with video and/or image
    public Submission(HafalanTask task, Program program, ProgramType programType, UserAccount member, String note, String recordingUrl, String imageUrl) {
        this.task = task;
        this.program = program;
        this.programType = programType != null ? programType : (task != null ? ProgramType.TAHFIDZ : ProgramType.TAHLIL);
        this.member = member;
        this.note = note;
        this.recordingUrl = recordingUrl;
        this.imageUrl = imageUrl;
        this.submittedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTaskId() { return task != null ? task.getId() : null; }
    public HafalanTask getTask() { return task; }
    public UUID getTahlilTaskId() { return tahlilTask != null ? tahlilTask.getId() : null; }
    public TahlilTask getTahlilTask() { return tahlilTask; }
    public void setTahlilTask(TahlilTask tahlilTask) { this.tahlilTask = tahlilTask; }
    public UUID getProgramId() { return program != null ? program.getId() : (task != null ? task.getProgramId() : null); }
    public Program getProgram() { return program; }
    public ProgramType getProgramType() { return programType != null ? programType : (task != null ? ProgramType.TAHFIDZ : ProgramType.TAHLIL); }
    public UUID getMemberId() { return member.getId(); }
    public UserAccount getMember() { return member; }
    public String getNote() { return note; }
    public String getRecordingUrl() { return recordingUrl; }
    public String getImageUrl() { return imageUrl; }
    public SubmissionStatus getStatus() { return status; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getSubmittedAt() { return submittedAt; }

    public void review(UserAccount reviewer, SubmissionStatus status, String note) {
        this.reviewedBy = reviewer;
        this.status = status;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }
}
