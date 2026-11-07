package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "halaqah_attendance", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"session_id", "user_id"})
})
public class HalaqahAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private HalaqahSession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(nullable = false)
    private int watchedSeconds = 0;

    @Column(nullable = false)
    private boolean completed = false;

    private Instant attendedAt;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected HalaqahAttendance() {}

    public HalaqahAttendance(HalaqahSession session, UserAccount user, int watchedSeconds, boolean completed, String notes) {
        this.session = session;
        this.user = user;
        this.watchedSeconds = watchedSeconds;
        this.completed = completed;
        if (completed) {
            this.attendedAt = Instant.now();
        }
        this.notes = notes;
    }

    public UUID getId() { return id; }
    public HalaqahSession getSession() { return session; }
    public UserAccount getUser() { return user; }
    public int getWatchedSeconds() { return watchedSeconds; }
    public boolean isCompleted() { return completed; }
    public Instant getAttendedAt() { return attendedAt; }
    public String getNotes() { return notes; }
    public Instant getCreatedAt() { return createdAt; }

    public void updateProgress(int watchedSeconds, boolean completed, String notes) {
        if (watchedSeconds > this.watchedSeconds) {
            this.watchedSeconds = watchedSeconds;
        }
        if (completed && !this.completed) {
            this.completed = true;
            this.attendedAt = Instant.now();
        }
        if (notes != null) {
            this.notes = notes;
        }
    }
}
