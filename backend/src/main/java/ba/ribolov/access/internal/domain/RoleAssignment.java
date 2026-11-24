package ba.ribolov.access.internal.domain;

import ba.ribolov.access.api.Role;
import ba.ribolov.organizations.model.Organization;
import ba.ribolov.organizations.model.OrganizationType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "role_assignment", schema = "access")
public class RoleAssignment {
    @Id
    private UUID id;
    @Version
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_role_user"))
    private ApplicationUser user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", foreignKey = @ForeignKey(name = "fk_role_organization"))
    private Organization organization;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Role role;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_until")
    private Instant validUntil;
    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RoleAssignment() {}

    public RoleAssignment(ApplicationUser user, Role role, Organization organization, Instant validFrom, Instant validUntil) {
        this.user = Objects.requireNonNull(user);
        this.role = Objects.requireNonNull(role);
        this.validFrom = Objects.requireNonNull(validFrom);
        if (!user.isActive()) throw new IllegalArgumentException("Inactive user");
        if (validUntil != null && !validUntil.isAfter(validFrom))
            throw new IllegalArgumentException("Role assignment period must be nonempty");
        validateScope(role, organization);
        this.id = UUID.randomUUID();
        this.organization = organization;
        this.validUntil = validUntil;
        user.addRoleAssignment(this);
    }

    public static void validateScope(Role role, Organization organization) {
        OrganizationType requiredType = switch (role) {
            case MINISTRY_ANALYST -> OrganizationType.MINISTRY;
            case FEDERATION_ANALYST -> OrganizationType.FEDERATION;
            case ASSOCIATION_ADMIN, OPERATOR, WARDEN -> OrganizationType.ASSOCIATION;
            case SYSTEM_ADMIN, ANGLER -> null;
        };
        if (requiredType == null ? organization != null
                : organization == null || organization.getType() != requiredType || !organization.isActive())
            throw new IllegalArgumentException("Role and organization scope do not match");
    }

    public boolean isActiveAt(Instant instant) {
        return user.isActive() && (organization == null || organization.isActive())
                && !instant.isBefore(validFrom)
                && (validUntil == null || instant.isBefore(validUntil))
                && (revokedAt == null || instant.isBefore(revokedAt));
    }

    public void revoke(Instant at) {
        Objects.requireNonNull(at);
        if (revokedAt == null) revokedAt = at;
    }

    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public ApplicationUser getUser() { return user; }
    public Organization getOrganization() { return organization; }
    public Role getRole() { return role; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public Instant getRevokedAt() { return revokedAt; }
}
