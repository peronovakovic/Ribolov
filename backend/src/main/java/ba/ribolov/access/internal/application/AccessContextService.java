package ba.ribolov.access.internal.application;

import ba.ribolov.access.api.AccessContextView;
import ba.ribolov.access.internal.domain.RoleAssignment;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AccessContextService {
    private final EntityManager entityManager;
    private final Clock clock;
    private final boolean databaseIdentityContext;

    public AccessContextService(EntityManager entityManager, Clock clock,
            @Value("${ribolov.database.identity-context:true}") boolean databaseIdentityContext) {
        this.entityManager = entityManager;
        this.clock = clock;
        this.databaseIdentityContext = databaseIdentityContext;
    }

    public List<AccessContextView> available(String issuer, String subject) {
        if (issuer == null || issuer.isBlank() || subject == null || subject.isBlank())
            throw new AccessDeniedException("Verified identity is required");
        if (databaseIdentityContext) {
            entityManager.createNativeQuery("""
                    select set_config('ribolov.issuer', :issuer, true),
                           set_config('ribolov.subject', :subject, true)
                    """).setParameter("issuer", issuer).setParameter("subject", subject).getSingleResult();
        }
        return entityManager.createQuery("""
                select r from RoleAssignment r join fetch r.user u left join fetch r.organization o
                where u.issuer = :issuer and u.subject = :subject and u.active = true
                  and r.validFrom <= :now and (r.validUntil is null or r.validUntil > :now)
                  and (r.revokedAt is null or r.revokedAt > :now)
                  and (o is null or o.status = ba.ribolov.organizations.model.OrganizationStatus.ACTIVE)
                order by r.role, o.name, r.id
                """, RoleAssignment.class)
                .setParameter("issuer", issuer).setParameter("subject", subject)
                .setParameter("now", clock.instant()).getResultList().stream()
                .map(this::view).toList();
    }

    /** Re-evaluate on every operation; knowing an assignment UUID does not grant access. */
    public AccessContextView require(String issuer, String subject, UUID assignmentId) {
        return available(issuer, subject).stream().filter(context -> context.id().equals(assignmentId))
                .findFirst().orElseThrow(() -> new AccessDeniedException("Access context is unavailable"));
    }

    private AccessContextView view(RoleAssignment assignment) {
        var organization = assignment.getOrganization();
        return new AccessContextView(assignment.getId(), assignment.getRole(),
                organization == null ? null : new AccessContextView.OrganizationView(organization.getId(), organization.getName()),
                assignment.getValidFrom(), assignment.getValidUntil());
    }
}
