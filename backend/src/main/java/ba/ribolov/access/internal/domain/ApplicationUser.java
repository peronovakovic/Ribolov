package ba.ribolov.access.internal.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "application_user", schema = "access",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_identity", columnNames = {"issuer", "subject"}))
public class ApplicationUser {
    @Id
    private UUID id;
    @Version
    private Long version;
    // External OIDC identity, not a relation to another local entity.
    @Column(nullable = false, length = 500, updatable = false)
    private String issuer;
    @Column(nullable = false, length = 255, updatable = false)
    private String subject;
    @Column(name = "display_name", nullable = false, length = 240)
    private String displayName = "Korisnik";
    @Column(nullable = false)
    private boolean active;
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<RoleAssignment> roleAssignments = new ArrayList<>();

    protected ApplicationUser() {}

    public ApplicationUser(String issuer, String subject) {
        if (issuer == null || issuer.isBlank() || issuer.length() > 500
                || subject == null || subject.isBlank() || subject.length() > 255)
            throw new IllegalArgumentException("OIDC issuer and subject are required");
        this.id = UUID.randomUUID();
        this.issuer = issuer;
        this.subject = subject;
        this.active = true;
    }

    void addRoleAssignment(RoleAssignment assignment) { roleAssignments.add(assignment); }
    public void updateProfile(String name, boolean active) {
        if (name == null || name.isBlank() || name.strip().length() > 240)
            throw new IllegalArgumentException("Ime korisnika mora imati od 1 do 240 znakova.");
        this.displayName = name.strip();
        this.active = active;
    }
    public String getDisplayName() { return displayName; }
    public void deactivate() { active = false; }
    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public String getIssuer() { return issuer; }
    public String getSubject() { return subject; }
    public boolean isActive() { return active; }
    public List<RoleAssignment> getRoleAssignments() { return Collections.unmodifiableList(roleAssignments); }
}
