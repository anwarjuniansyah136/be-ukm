package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "halaqah_sessions")
public class HalaqahSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false, length = 1000)
    private String videoUrl;

    @Column(nullable = false)
    private int durationSeconds = 300;

    private String speaker;

    private LocalDate scheduledDate;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected HalaqahSession() {}

    public HalaqahSession(String title, String description, String videoUrl, int durationSeconds, String speaker, LocalDate scheduledDate, UserAccount createdBy) {
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        this.durationSeconds = durationSeconds > 0 ? durationSeconds : 300;
        this.speaker = speaker;
        this.scheduledDate = scheduledDate != null ? scheduledDate : LocalDate.now();
        this.createdBy = createdBy;
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getVideoUrl() { return videoUrl; }
    public int getDurationSeconds() { return durationSeconds; }
    public String getSpeaker() { return speaker; }
    public LocalDate getScheduledDate() { return scheduledDate; }
    public UserAccount getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }

    public void update(String title, String description, String videoUrl, int durationSeconds, String speaker, LocalDate scheduledDate) {
        this.title = title;
        this.description = description;
        this.videoUrl = videoUrl;
        if (durationSeconds > 0) this.durationSeconds = durationSeconds;
        this.speaker = speaker;
        if (scheduledDate != null) this.scheduledDate = scheduledDate;
    }
}
