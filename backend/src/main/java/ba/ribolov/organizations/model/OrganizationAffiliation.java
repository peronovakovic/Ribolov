package ba.ribolov.organizations.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "organization_affiliation", schema = "organizations")
public class OrganizationAffiliation {
    @Id
    private UUID id;
    @Version
    private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false, foreignKey = @ForeignKey(name = "fk_affiliation_child"))
    private Organization child;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false, foreignKey = @ForeignKey(name = "fk_affiliation_parent"))
    private Organization parent;
    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;
    @Column(name = "valid_until")
    private Instant validUntil;

    protected OrganizationAffiliation() {}

    public OrganizationAffiliation(Organization child, Organization parent, Instant validFrom, Instant validUntil) {
        this.child = Objects.requireNonNull(child);
        this.parent = Objects.requireNonNull(parent);
        this.validFrom = Objects.requireNonNull(validFrom);
        if (validUntil != null && !validUntil.isAfter(validFrom))
            throw new IllegalArgumentException("Affiliation period must be nonempty");
        if (child.getId().equals(parent.getId()) || !child.getType().acceptsParent(parent.getType()))
            throw new IllegalArgumentException("Invalid organization hierarchy");
        if (!child.isActive() || !parent.isActive())
            throw new IllegalArgumentException("Archived organizations cannot receive new affiliations");
        this.id = UUID.randomUUID();
        this.validUntil = validUntil;
        child.addParentAffiliation(this);
        parent.addChildAffiliation(this);
    }

    public void endAt(Instant end) {
        Objects.requireNonNull(end);
        if (!end.isAfter(validFrom) || (validUntil != null && end.isAfter(validUntil)))
            throw new IllegalArgumentException("An affiliation can only be shortened to a nonempty period");
        validUntil = end;
    }

    public boolean includes(Instant instant) {
        return !instant.isBefore(validFrom) && (validUntil == null || instant.isBefore(validUntil));
    }
    public UUID getId() { return id; }
    public Long getVersion() { return version; }
    public Organization getChild() { return child; }
    public Organization getParent() { return parent; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
}
