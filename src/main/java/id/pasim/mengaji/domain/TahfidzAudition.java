package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tahfidz_auditions")
public class TahfidzAudition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id")
    private UserAccount member;

    @Column(nullable = false)
    private String randomSurah;

    @Column(nullable = false)
    private int randomAyatStart;

    @Column(nullable = false)
    private int randomAyatEnd;

    @Column(length = 1000)
    private String recordingUrl;

    @Column(length = 2000)
    private String memberNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditionStatus status = AuditionStatus.MENUNGGU_REVIEW;

    @ManyToOne
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;

    @Column(length = 2000)
    private String reviewNote;

    private Instant reviewedAt;

    // Target duration selection after audition approved
    private String targetSurah;
    private Integer targetAyatStart;
    private Integer targetAyatEnd;
    private Integer chosenDurationDays;

    @Enumerated(EnumType.STRING)
    private AuditionStatus targetApprovalStatus;

    @ManyToOne
    @JoinColumn(name = "target_reviewed_by")
    private UserAccount targetReviewedBy;

    @Column(length = 2000)
    private String targetReviewNote;

    private Instant targetReviewedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected TahfidzAudition() {}

    public TahfidzAudition(UserAccount member, String randomSurah, int randomAyatStart, int randomAyatEnd, String recordingUrl, String memberNote) {
        this.member = member;
        this.randomSurah = randomSurah;
        this.randomAyatStart = randomAyatStart;
        this.randomAyatEnd = randomAyatEnd;
        this.recordingUrl = recordingUrl;
        this.memberNote = memberNote;
        this.status = AuditionStatus.MENUNGGU_REVIEW;
    }

    public void review(UserAccount reviewer, AuditionStatus newStatus, String note) {
        this.reviewedBy = reviewer;
        this.status = newStatus;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public void chooseTarget(String surah, int ayatStart, int ayatEnd, int durationDays) {
        this.targetSurah = surah;
        this.targetAyatStart = ayatStart;
        this.targetAyatEnd = ayatEnd;
        this.chosenDurationDays = durationDays;
        this.targetApprovalStatus = AuditionStatus.MENUNGGU_REVIEW;
    }

    public void reviewTarget(UserAccount reviewer, AuditionStatus newStatus, String note) {
        this.targetReviewedBy = reviewer;
        this.targetApprovalStatus = newStatus;
        this.targetReviewNote = note;
        this.targetReviewedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UserAccount getMember() { return member; }
    public String getRandomSurah() { return randomSurah; }
    public int getRandomAyatStart() { return randomAyatStart; }
    public int getRandomAyatEnd() { return randomAyatEnd; }
    public String getRecordingUrl() { return recordingUrl; }
    public String getMemberNote() { return memberNote; }
    public AuditionStatus getStatus() { return status; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public String getReviewNote() { return reviewNote; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getTargetSurah() { return targetSurah; }
    public Integer getTargetAyatStart() { return targetAyatStart; }
    public Integer getTargetAyatEnd() { return targetAyatEnd; }
    public Integer getChosenDurationDays() { return chosenDurationDays; }
    public AuditionStatus getTargetApprovalStatus() { return targetApprovalStatus; }
    public UserAccount getTargetReviewedBy() { return targetReviewedBy; }
    public String getTargetReviewNote() { return targetReviewNote; }
    public Instant getTargetReviewedAt() { return targetReviewedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
