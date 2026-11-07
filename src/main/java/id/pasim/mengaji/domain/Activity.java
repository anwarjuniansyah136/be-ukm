package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "activities")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 3000)
    private String description;

    @Column(nullable = false)
    private LocalDate eventDate;

    private String eventTime;

    private String location;

    private String speaker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityStatus status = ActivityStatus.DIJADWALKAN;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Activity() {}

    public Activity(String title, String description, LocalDate eventDate, String eventTime, String location, String speaker, ActivityStatus status, UserAccount createdBy) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.speaker = speaker;
        this.status = status != null ? status : ActivityStatus.DIJADWALKAN;
        this.createdBy = createdBy;
    }

    public void update(String title, String description, LocalDate eventDate, String eventTime, String location, String speaker, ActivityStatus status) {
        this.title = title;
        this.description = description;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.location = location;
        this.speaker = speaker;
        if (status != null) {
            this.status = status;
        }
    }

    public void updateStatus(ActivityStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getEventDate() { return eventDate; }
    public String getEventTime() { return eventTime; }
    public String getLocation() { return location; }
    public String getSpeaker() { return speaker; }
    public ActivityStatus getStatus() { return status; }
    public UserAccount getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
