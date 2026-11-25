package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tahlil_tasks")
public class TahlilTask {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "program_id")
    private Program program;

    // Optional: null means broadcast for all active Tahlil members
    @ManyToOne(optional = true)
    @JoinColumn(name = "member_id", nullable = true)
    private UserAccount member;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 2000)
    private String instruction;

    @Column(nullable = false)
    private LocalDate dueDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected TahlilTask() {}

    public TahlilTask(Program program, UserAccount member, String title, String instruction, LocalDate dueDate, UserAccount createdBy) {
        this.program = program;
        this.member = member;
        this.title = title;
        this.instruction = instruction;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Program getProgram() { return program; }
    public UUID getProgramId() { return program != null ? program.getId() : null; }
    public UserAccount getMember() { return member; }
    public UUID getMemberId() { return member != null ? member.getId() : null; }
    public String getTitle() { return title; }
    public String getInstruction() { return instruction; }
    public LocalDate getDueDate() { return dueDate; }
    public UserAccount getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
