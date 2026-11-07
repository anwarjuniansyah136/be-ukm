package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name="hafalan_tasks")
public class HafalanTask {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="program_id") private Program program;
    @ManyToOne(optional=false) @JoinColumn(name="member_id") private UserAccount member;
    @Column(nullable=false) private String surah;
    @Column(nullable=false) private int ayatStart;
    @Column(nullable=false) private int ayatEnd;
    private Integer durationDays;
    @Column(nullable=false) private String allowedDurations;
    @Version private long version;
    @Column(nullable=false) private LocalDate startDate;
    private LocalDate dueDate;
    @Column(length=1000) private String note;
    @ManyToOne(optional=false) @JoinColumn(name="assigned_by") private UserAccount assignedBy;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected HafalanTask(){}
    public HafalanTask(Program p,UserAccount m,String surah,int from,int to,String allowed,LocalDate start,String note,UserAccount by){this.program=p;this.member=m;this.surah=surah;this.ayatStart=from;this.ayatEnd=to;this.allowedDurations=allowed;this.startDate=start;this.note=note;this.assignedBy=by;}
    public java.util.List<Integer> getAllowedDurations(){return java.util.Arrays.stream(allowedDurations.split(",")).map(Integer::valueOf).toList();}
    public void chooseDuration(int days){this.durationDays=days;this.dueDate=startDate.plusDays(days);}
    public UUID getId(){return id;} public UUID getProgramId(){return program.getId();} public UUID getMemberId(){return member.getId();} public String getSurah(){return surah;} public int getAyatStart(){return ayatStart;} public int getAyatEnd(){return ayatEnd;} public Integer getDurationDays(){return durationDays;} public LocalDate getStartDate(){return startDate;} public LocalDate getDueDate(){return dueDate;} public String getNote(){return note;} public Instant getCreatedAt(){return createdAt;}
}
