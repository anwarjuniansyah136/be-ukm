package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="programs")
public class Program {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ProgramType type;
    @Column(length=2000) private String description;
    @Column(nullable=false) private boolean active=true;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    private java.time.OffsetDateTime scheduledAt;
    private String location;
    private String groupName;
    @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="program_members",joinColumns=@JoinColumn(name="program_id")) @Column(name="member_id")
    private java.util.Set<UUID> participants=new java.util.HashSet<>();
    public java.time.OffsetDateTime getScheduledAt(){return scheduledAt;}
    public String getLocation(){return location;}
    public String getGroupName(){return groupName;}
    public java.util.Set<UUID> getParticipants(){return participants;}
    public void join(UUID member){participants.add(member);}
    public void leave(UUID member){participants.remove(member);}
    public void update(String name,ProgramType type,String description,java.time.OffsetDateTime when,String location,String group,boolean active){this.name=name;this.type=type;this.description=description;this.scheduledAt=when;this.location=location;this.groupName=group;this.active=active;}
    protected Program(){}
    public Program(String name,ProgramType type,String description){this.name=name;this.type=type;this.description=description;}
    public UUID getId(){return id;} public String getName(){return name;} public ProgramType getType(){return type;} public String getDescription(){return description;} public boolean isActive(){return active;} public Instant getCreatedAt(){return createdAt;}
}
