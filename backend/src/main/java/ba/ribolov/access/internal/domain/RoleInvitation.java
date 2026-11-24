package ba.ribolov.access.internal.domain;

import ba.ribolov.access.api.Role;
import ba.ribolov.organizations.model.Organization;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

@Entity
@org.hibernate.annotations.DynamicUpdate
@Table(name = "role_invitation", schema = "access")
public class RoleInvitation {
    @Id private UUID id;
    @Version private Long version;
    @Column(nullable=false, length=254, updatable=false) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=40,updatable=false) private Role role;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="organization_id",nullable=false,updatable=false) private Organization organization;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="invited_by_id",nullable=false,updatable=false) private RoleAssignment invitedBy;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="accepted_assignment_id") private RoleAssignment acceptedAssignment;
    @Column(name="token_hash",nullable=false,length=64,unique=true,updatable=false) private String tokenHash;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="expires_at",nullable=false,updatable=false) private Instant expiresAt;
    @Column(name="valid_until",updatable=false) private Instant validUntil;
    @Column(name="accepted_at") private Instant acceptedAt;
    @Column(name="revoked_at") private Instant revokedAt;
    @Column(name="sent_at") private Instant sentAt;
    @Column(nullable=false) private int attempts;
    @Column(name="next_attempt_at",nullable=false) private Instant nextAttemptAt;
    @Column(name="delivery_failure",length=30) private String deliveryFailure;

    protected RoleInvitation() {}
    public RoleInvitation(UUID id, String email, Role role, Organization organization, RoleAssignment invitedBy,
                          String tokenHash, Instant now, Instant validUntil) {
        this.id=id; this.email=normalizeEmail(email); this.role=role; this.organization=organization; this.invitedBy=invitedBy;
        this.tokenHash=tokenHash; this.createdAt=now; this.expiresAt=now.plus(Duration.ofHours(48)); this.nextAttemptAt=now;
        if (validUntil!=null && !validUntil.isAfter(expiresAt))
            throw new IllegalArgumentException("Kraj ovlaĹˇtenja mora biti poslije isteka poziva (48 sati).");
        this.validUntil=validUntil;
        // Validate scope with the same domain invariant as the resulting assignment.
        RoleAssignment.validateScope(role, organization);
    }
    public static String normalizeEmail(String value) {
        if(value==null) throw new IllegalArgumentException("Unesite adresu e-poĹˇte.");
        String email=value.strip().toLowerCase(Locale.ROOT);
        if(email.length()>254 || !email.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?\\.[a-z]{2,63}"))
            throw new IllegalArgumentException("Adresa e-poĹˇte nije ispravna.");
        return email;
    }
    public boolean pendingAt(Instant now) { return acceptedAt==null && revokedAt==null && now.isBefore(expiresAt); }
    public boolean authorizesAt(Instant now) {
        return invitedBy.isActiveAt(now) && organization.isActive() &&
            (invitedBy.getRole()==Role.SYSTEM_ADMIN
                ? role==Role.MINISTRY_ANALYST || role==Role.FEDERATION_ANALYST || role==Role.ASSOCIATION_ADMIN
                : invitedBy.getRole()==Role.ASSOCIATION_ADMIN && invitedBy.getOrganization().getId().equals(organization.getId())
                  && (role==Role.OPERATOR || role==Role.WARDEN));
    }
    public void accept(RoleAssignment assignment, Instant now) {
        if(!pendingAt(now)) throw new IllegalStateException("Poziv viĹˇe nije vaĹľeÄ‡i.");
        acceptedAssignment=assignment; acceptedAt=now;
    }
    public void revoke(Instant now) {
        if(!pendingAt(now)) throw new IllegalStateException("Poziv viĹˇe nije na ÄŤekanju.");
        revokedAt=now;
    }
    public void delivered(Instant now) { attempts++; sentAt=now; deliveryFailure=null; }
    public void failed(Instant now) { attempts++; deliveryFailure="DELIVERY_FAILED"; nextAttemptAt=now.plusSeconds(Math.min(3600,60L << Math.min(attempts,6))); }
    public void retry(Instant now) {
        if(!pendingAt(now) || !authorizesAt(now)) throw new IllegalStateException("Poziv viĹˇe nije vaĹľeÄ‡i.");
        attempts=0; sentAt=null; deliveryFailure=null; nextAttemptAt=now;
    }
    public String status(Instant now) {
        if(acceptedAt!=null) return "ACCEPTED"; if(revokedAt!=null) return "REVOKED";
        if(!now.isBefore(expiresAt)) return "EXPIRED"; return "PENDING";
    }
    public UUID getId(){return id;} public Long getVersion(){return version;} public String getEmail(){return email;}
    public Role getRole(){return role;} public Organization getOrganization(){return organization;} public RoleAssignment getInvitedBy(){return invitedBy;}
    public RoleAssignment getAcceptedAssignment(){return acceptedAssignment;} public String getTokenHash(){return tokenHash;}
    public Instant getCreatedAt(){return createdAt;} public Instant getExpiresAt(){return expiresAt;} public Instant getValidUntil(){return validUntil;}
    public Instant getAcceptedAt(){return acceptedAt;} public Instant getRevokedAt(){return revokedAt;} public Instant getSentAt(){return sentAt;}
    public int getAttempts(){return attempts;} public String getDeliveryFailure(){return deliveryFailure;}
}
