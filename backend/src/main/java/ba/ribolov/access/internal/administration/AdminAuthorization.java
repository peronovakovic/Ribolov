package ba.ribolov.access.internal.administration;

import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.application.AccessContextService;
import ba.ribolov.access.internal.domain.RoleAssignment;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class AdminAuthorization {
    private final AccessContextService contexts;
    private final EntityManager em;
    private final Clock clock;
    private final boolean databaseContext;
    public AdminAuthorization(AccessContextService contexts, EntityManager em, Clock clock,
            @Value("${ribolov.database.identity-context:true}") boolean databaseContext) {
        this.contexts = contexts; this.em = em; this.clock = clock; this.databaseContext = databaseContext;
    }

    public RoleAssignment require(AdminCaller caller, UUID contextId, boolean mutation) {
        if (contextId == null) throw new AccessDeniedException("Odaberite aktivnu ulogu.");
        var context = contexts.require(caller.issuer(), caller.subject(), contextId);
        if (context.role() != Role.SYSTEM_ADMIN && context.role() != Role.ASSOCIATION_ADMIN)
            throw new AccessDeniedException("Izabrana uloga nema administrativni pristup.");
        if (!"2".equals(caller.acr())) throw new ReauthenticationRequired();
        var now = clock.instant();
        if (mutation && (caller.authenticatedAt() == null
                || caller.authenticatedAt().isBefore(now.minusSeconds(300))
                || caller.authenticatedAt().isAfter(now.plusSeconds(30)))) throw new ReauthenticationRequired();
        var assignment = em.find(RoleAssignment.class, contextId);
        if (databaseContext) {
            em.createNativeQuery("""
                    select set_config('ribolov.assignment_id', :id, true), set_config('ribolov.acr', '2', true),
                           set_config('ribolov.auth_time', :time, true), set_config('ribolov.target_user_id', '', true)
                    """).setParameter("id", contextId.toString())
                    .setParameter("time", caller.authenticatedAt() == null ? "" : Long.toString(caller.authenticatedAt().getEpochSecond()))
                    .getSingleResult();
        }
        return assignment;
    }

    public void system(RoleAssignment context) {
        if (context.getRole() != Role.SYSTEM_ADMIN) throw new AccessDeniedException("Radnja zahtijeva sistemskog administratora.");
    }

    public void organization(RoleAssignment context, UUID id) {
        if (context.getRole() != Role.SYSTEM_ADMIN && !context.getOrganization().getId().equals(id))
            throw new AccessDeniedException("Organizacija nije dostupna u izabranoj ulozi.");
    }

    public void role(RoleAssignment context, Role targetRole, UUID organization) {
        boolean allowed = context.getRole() == Role.SYSTEM_ADMIN
                ? targetRole == Role.MINISTRY_ANALYST || targetRole == Role.FEDERATION_ANALYST || targetRole == Role.ASSOCIATION_ADMIN
                : (targetRole == Role.OPERATOR || targetRole == Role.WARDEN) && context.getOrganization().getId().equals(organization);
        if (!allowed) throw new AccessDeniedException("Izabrana uloga ne može dodijeliti ili opozvati ovo ovlaštenje.");
    }

    public void exactTargetUser(UUID userId) {
        if (userId == null) throw new IllegalArgumentException("Korisnik je obavezan.");
        if (databaseContext) em.createNativeQuery("select set_config('ribolov.target_user_id', :id, true)")
                .setParameter("id", userId.toString()).getSingleResult();
    }
}
