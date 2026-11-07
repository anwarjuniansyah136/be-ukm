package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="materials")
public class Material {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) @JoinColumn(name="program_id") private Program program;
    @Column(nullable=false) private String title;
    @Column(length=4000) private String body;
    private String resourceUrl;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    protected Material(){}
    public Material(Program program,String title,String body,String resourceUrl){this.program=program;this.title=title;this.body=body;this.resourceUrl=resourceUrl;}
    public void update(Program program,String title,String body,String resourceUrl){this.program=program;this.title=title;this.body=body;this.resourceUrl=resourceUrl;}
    public UUID getId(){return id;} public UUID getProgramId(){return program.getId();} public String getTitle(){return title;} public String getBody(){return body;} public String getResourceUrl(){return resourceUrl;} public Instant getCreatedAt(){return createdAt;}
}
