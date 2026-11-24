package ba.ribolov.access.internal.domain;

import ba.ribolov.organizations.model.Organization;
import ba.ribolov.organizations.model.OrganizationAffiliation;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "administration_event", schema = "access")
public class AdministrationEvent {
    public enum Action { BOOTSTRAP_ADMIN, ORGANIZATION_CREATED, ORGANIZATION_UPDATED, PARENT_CHANGED, USER_CONNECTED, USER_UPDATED, ROLE_GRANTED, ROLE_REVOKED, INVITATION_CREATED, INVITATION_REVOKED, INVITATION_RESENT, INVITATION_ACCEPTED }
    @Id private UUID id;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false, updatable = false) private ApplicationUser actor;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", updatable = false) private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", updatable = false) private ApplicationUser targetUser;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", updatable = false) private RoleAssignment assignment;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affiliation_id", updatable = false) private OrganizationAffiliation affiliation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false) private Action action;
    @Column(nullable = false, length = 1000, updatable = false) private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitation_id", updatable = false) private RoleInvitation invitation;

    protected AdministrationEvent() {}
    public void linkInvitation(RoleInvitation invitation) { this.invitation = java.util.Objects.requireNonNull(invitation); }
    public RoleInvitation getInvitation() { return invitation; }
    public AdministrationEvent(ApplicationUser actor, Organization organization, ApplicationUser targetUser,
            RoleAssignment assignment, OrganizationAffiliation affiliation, Action action, String reason, Instant now) {
        if (actor == null || action == null || now == null || reason == null || reason.isBlank() || reason.strip().length() > 1000)
            throw new IllegalArgumentException("Unesite razlog promjene, najviĹˇe 1000 znakova.");
        this.id = UUID.randomUUID(); this.actor = actor; this.organization = organization; this.targetUser = targetUser;
        this.assignment = assignment; this.affiliation = affiliation; this.action = action; this.reason = reason.strip(); this.occurredAt = now;
    }
    public UUID getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public ApplicationUser getActor() { return actor; }
    public Organization getOrganization() { return organization; }
    public ApplicationUser getTargetUser() { return targetUser; }
    public RoleAssignment getAssignment() { return assignment; }
    public OrganizationAffiliation getAffiliation() { return affiliation; }
    public Action getAction() { return action; }
    public String getReason() { return reason; }
}
