package ba.ribolov.access.internal.administration;

import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.domain.*;
import ba.ribolov.organizations.api.OrganizationManagement;
import ba.ribolov.organizations.model.Organization;
import jakarta.persistence.*;
import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static ba.ribolov.access.internal.domain.AdministrationEvent.Action.*;

@Service
@Transactional
public class AdministrationService {
    private final EntityManager em;
    private final AdminAuthorization authorization;
    private final OrganizationManagement organizations;
    private final Clock clock;
    private final String issuer;
    public AdministrationService(EntityManager em, AdminAuthorization authorization, OrganizationManagement organizations,
            Clock clock, @Value("${ribolov.security.issuer}") String issuer) {
        this.em = em; this.authorization = authorization; this.organizations = organizations; this.clock = clock; this.issuer = issuer;
    }

    @Transactional(readOnly = true)
    public OrganizationManagement.Page organizations(AdminCaller caller, UUID contextId, String search, int page) {
        var context = authorization.require(caller, contextId, false);
        return organizations.list(context.getRole() == Role.SYSTEM_ADMIN ? null : context.getOrganization().getId(), search, page, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    }
    @Transactional(readOnly = true)
    public List<OrganizationManagement.HistoryView> history(AdminCaller caller, UUID contextId, UUID id) {
        authorization.system(authorization.require(caller, contextId, false));
        return organizations.history(id);
    }
    public OrganizationManagement.View createOrganization(AdminCaller caller, UUID contextId, OrganizationManagement.Create request, String reason) {
        var context = authorization.require(caller, contextId, true); authorization.system(context);
        var result = organizations.create(request, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        record(context, result.organization(), null, null, result.affiliation(), ORGANIZATION_CREATED, reason);
        return result.view();
    }
    public OrganizationManagement.View updateOrganization(AdminCaller caller, UUID contextId, UUID id, OrganizationManagement.Update request, String reason) {
        var context = authorization.require(caller, contextId, true); authorization.organization(context, id);
        var result = organizations.update(id, request, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        record(context, result.organization(), null, null, null, ORGANIZATION_UPDATED, reason);
        return result.view();
    }
    public OrganizationManagement.View changeParent(AdminCaller caller, UUID contextId, UUID id, OrganizationManagement.ChangeParent request, String reason) {
        var context = authorization.require(caller, contextId, true); authorization.system(context);
        var result = organizations.changeParent(id, request, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        record(context, result.organization(), null, null, result.affiliation(), PARENT_CHANGED, reason);
        return result.view();
    }

    @Transactional(readOnly = true)
    public AdminViews.Page<AdminViews.User> users(AdminCaller caller, UUID contextId, String search, int page) {
        var context = authorization.require(caller, contextId, false);
        var q = em.createQuery("select u from ApplicationUser u where lower(u.displayName) like :search escape '!'"
                + (context.getRole() == Role.SYSTEM_ADMIN ? "" : " and exists (select r.id from RoleAssignment r where r.user = u and r.organization.id = :organization)")
                + " order by u.displayName, u.id", ApplicationUser.class).setParameter("search", pattern(search))
                .setFirstResult(offset(page)).setMaxResults(51);
        if (context.getRole() != Role.SYSTEM_ADMIN) q.setParameter("organization", context.getOrganization().getId());
        var users = q.getResultList();
        return new AdminViews.Page<>(users.stream().limit(50).map(this::userView).toList(), page, users.size() > 50);
    }
    public AdminViews.User createUser(AdminCaller caller, UUID contextId, AdminViews.CreateUser request) {
        var context = authorization.require(caller, contextId, true); authorization.system(context);
        var user = new ApplicationUser(issuer, request.subject());
        user.updateProfile(request.displayName(), true);
        em.persist(user); em.flush();
        record(context, null, user, null, null, USER_CONNECTED, request.reason());
        return userView(user);
    }
    public AdminViews.User updateUser(AdminCaller caller, UUID contextId, UUID id, AdminViews.UpdateUser request) {
        var context = authorization.require(caller, contextId, true); authorization.system(context);
        var user = requireUser(id);
        version(user.getVersion(), request.version());
        if (!request.active() && em.createQuery("select count(r) from RoleAssignment r where r.user = :user and r.role = :role", Long.class)
                .setParameter("user", user).setParameter("role", Role.SYSTEM_ADMIN).getSingleResult() > 0)
            throw new IllegalArgumentException("Sistemski administrator ne može se deaktivirati kroz aplikaciju.");
        user.updateProfile(request.displayName(), request.active()); em.flush();
        record(context, null, user, null, null, USER_UPDATED, request.reason());
        return userView(user);
    }

    @Transactional(readOnly = true)
    public AdminViews.Page<AdminViews.Assignment> assignments(AdminCaller caller, UUID contextId, int page) {
        var context = authorization.require(caller, contextId, false);
        var q = em.createQuery("select r from RoleAssignment r join fetch r.user left join fetch r.organization"
                + (context.getRole() == Role.SYSTEM_ADMIN ? "" : " where r.organization.id = :organization")
                + " order by r.validFrom desc, r.id", RoleAssignment.class).setFirstResult(offset(page)).setMaxResults(51);
        if (context.getRole() != Role.SYSTEM_ADMIN) q.setParameter("organization", context.getOrganization().getId());
        var roles = q.getResultList();
        return new AdminViews.Page<>(roles.stream().limit(50).map(this::assignmentView).toList(), page, roles.size() > 50);
    }
    public AdminViews.Assignment grant(AdminCaller caller, UUID contextId, AdminViews.Grant request) {
        var context = authorization.require(caller, contextId, true);
        authorization.role(context, request.role(), request.organizationId());
        authorization.exactTargetUser(request.userId());
        var user = requireUser(request.userId());
        var organization = request.organizationId() == null ? null : em.find(Organization.class, request.organizationId());
        if (request.organizationId() != null && organization == null) throw new EntityNotFoundException("Organizacija nije dostupna.");
        var now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var start = request.validFrom() == null ? now : request.validFrom();
        if (start.isBefore(now)) throw new IllegalArgumentException("Dodjela ne može djelovati retroaktivno.");
        var assignment = new RoleAssignment(user, request.role(), organization, start, request.validUntil());
        em.persist(assignment); em.flush();
        record(context, organization, user, assignment, null, ROLE_GRANTED, request.reason());
        return assignmentView(assignment);
    }
    public AdminViews.Assignment revoke(AdminCaller caller, UUID contextId, UUID id, AdminViews.Revoke request) {
        var context = authorization.require(caller, contextId, true);
        var assignment = em.find(RoleAssignment.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (assignment == null) throw new EntityNotFoundException("Dodjela nije dostupna.");
        authorization.role(context, assignment.getRole(), assignment.getOrganization() == null ? null : assignment.getOrganization().getId());
        version(assignment.getVersion(), request.version());
        if (assignment.getRevokedAt() != null) throw new IllegalStateException("Dodjela je već opozvana.");
        assignment.revoke(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS)); em.flush();
        record(context, assignment.getOrganization(), assignment.getUser(), assignment, null, ROLE_REVOKED, request.reason());
        return assignmentView(assignment);
    }

    @Transactional(readOnly = true)
    public AdminViews.Page<AdminViews.Event> events(AdminCaller caller, UUID contextId, int page) {
        var context = authorization.require(caller, contextId, false);
        var q = em.createQuery("select e from AdministrationEvent e join fetch e.actor left join fetch e.organization"
                + " left join fetch e.targetUser left join fetch e.assignment"
                + (context.getRole() == Role.SYSTEM_ADMIN ? "" : " where e.organization.id = :organization")
                + " order by e.occurredAt desc, e.id", AdministrationEvent.class).setFirstResult(offset(page)).setMaxResults(51);
        if (context.getRole() != Role.SYSTEM_ADMIN) q.setParameter("organization", context.getOrganization().getId());
        var events = q.getResultList();
        return new AdminViews.Page<>(events.stream().limit(50).map(e -> new AdminViews.Event(e.getId(), e.getOccurredAt(),
                e.getAction().name(), e.getActor().getDisplayName(), e.getOrganization() == null ? null : e.getOrganization().getName(),
                e.getTargetUser() == null ? null : e.getTargetUser().getDisplayName(),
                e.getAssignment() == null ? null : e.getAssignment().getRole(), e.getReason())).toList(), page, events.size() > 50);
    }

    private void record(RoleAssignment actor, Organization organization, ApplicationUser target,
            RoleAssignment assignment, ba.ribolov.organizations.model.OrganizationAffiliation affiliation,
            AdministrationEvent.Action action, String reason) {
        em.persist(new AdministrationEvent(actor.getUser(), organization, target, assignment, affiliation, action, reason, clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS)));
        em.flush();
    }
    private ApplicationUser requireUser(UUID id) {
        if (id == null) throw new IllegalArgumentException("Korisnik je obavezan.");
        var user = em.find(ApplicationUser.class, id);
        if (user == null) throw new EntityNotFoundException("Korisnik nije dostupan.");
        return user;
    }
    private void version(Long actual, Long expected) {
        if (expected == null || !expected.equals(actual)) throw new OptimisticLockException("Podaci su promijenjeni. Osvježite pregled.");
    }
    private int offset(int page) {
        if (page < 0 || page > 10000) throw new IllegalArgumentException("Neispravna stranica.");
        return page * 50;
    }
    private String pattern(String search) {
        return "%" + Objects.requireNonNullElse(search, "").strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
    private AdminViews.User userView(ApplicationUser user) {
        return new AdminViews.User(user.getId(), user.getVersion(), user.getSubject(), user.getDisplayName(), user.isActive());
    }
    private AdminViews.Assignment assignmentView(RoleAssignment assignment) {
        var organization = assignment.getOrganization();
        return new AdminViews.Assignment(assignment.getId(), assignment.getVersion(),
                new AdminViews.UserSummary(assignment.getUser().getId(), assignment.getUser().getDisplayName()),
                organization == null ? null : new OrganizationManagement.ParentView(organization.getId(), organization.getName()),
                assignment.getRole(), assignment.getValidFrom(), assignment.getValidUntil(), assignment.getRevokedAt());
    }
}
