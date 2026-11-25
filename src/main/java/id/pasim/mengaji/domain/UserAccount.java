package id.pasim.mengaji.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="users")
public class UserAccount {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false, unique=true)
    private String email;

    @Column(nullable=false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable=true, length=32)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private RegistrationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable=true, length=32)
    private Role requestedRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable=true, length=32)
    private ProgramType requestedProgram;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private MemberStatus memberStatus = MemberStatus.AKTIF_TAHLIL;

    private String phone;
    private String nim;
    private String prodi;

    @Column(length=2000)
    private String bio;

    @Column(length=1000)
    private String avatarUrl;

    private String resetToken;
    private Instant resetTokenExpiry;

    @Column(nullable=false)
    private Instant createdAt;

    protected UserAccount() {}

    public UserAccount(String name, String email, String passwordHash, Role role, Role requestedRole, RegistrationStatus status) {
        this.name = name;
        this.email = email.toLowerCase();
        this.passwordHash = passwordHash;
        this.role = role;
        this.requestedRole = requestedRole;
        this.status = status;
        this.memberStatus = MemberStatus.AKTIF_TAHLIL;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public RegistrationStatus getStatus() { return status; }
    public Role getRequestedRole() { return requestedRole; }
    public ProgramType getRequestedProgram() { return requestedProgram; }
    public MemberStatus getMemberStatus() { return memberStatus != null ? memberStatus : MemberStatus.AKTIF_TAHLIL; }
    public String getPhone() { return phone; }
    public String getNim() { return nim; }
    public String getProdi() { return prodi; }
    public String getBio() { return bio; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getResetToken() { return resetToken; }
    public Instant getResetTokenExpiry() { return resetTokenExpiry; }
    public Instant getCreatedAt() { return createdAt; }

    public void decide(Role role, RegistrationStatus status) {
        this.role = role;
        this.status = status;
    }

    public void updateMemberStatus(MemberStatus memberStatus) {
        this.memberStatus = memberStatus;
    }

    public void setRequestedProgram(ProgramType requestedProgram) {
        this.requestedProgram = requestedProgram;
    }

    public void updateProfile(String name, String phone, String nim, String prodi, String bio, String avatarUrl) {
        if (name != null && !name.isBlank()) this.name = name.trim();
        this.phone = phone != null ? phone.trim() : null;
        this.nim = nim != null ? nim.trim() : null;
        this.prodi = prodi != null ? prodi.trim() : null;
        this.bio = bio != null ? bio.trim() : null;
        this.avatarUrl = avatarUrl != null ? avatarUrl.trim() : null;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setResetToken(String resetToken, Instant resetTokenExpiry) {
        this.resetToken = resetToken;
        this.resetTokenExpiry = resetTokenExpiry;
    }
}
