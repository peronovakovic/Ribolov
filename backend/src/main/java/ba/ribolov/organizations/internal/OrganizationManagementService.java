package ba.ribolov.organizations.internal;

import ba.ribolov.organizations.api.OrganizationManagement;
import ba.ribolov.organizations.model.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(propagation = Propagation.MANDATORY)
class OrganizationManagementService implements OrganizationManagement {
    private final EntityManager em;
    OrganizationManagementService(EntityManager em) { this.em = em; }

    public Page list(UUID scope, String search, int page, Instant now) {
        if (page < 0 || page > 10000) throw new IllegalArgumentException("Neispravna stranica.");
        String pattern = "%" + Objects.requireNonNullElse(search, "").strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        var query = em.createQuery("select o from Organization o where lower(o.name) like :search escape '!'"
                + (scope == null ? "" : " and o.id = :scope") + " order by o.name, o.id", Organization.class)
                .setParameter("search", pattern).setFirstResult(page * 50).setMaxResults(51);
        if (scope != null) query.setParameter("scope", scope);
        var organizations = query.getResultList();
        boolean more = organizations.size() > 50;
        var items = organizations.stream().limit(50).map(o -> view(o, now)).toList();
        return new Page(items, page, more);
    }

    public List<HistoryView> history(UUID id) {
        require(id);
        return em.createQuery("select a from OrganizationAffiliation a join fetch a.parent where a.child.id = :id order by a.validFrom desc",
                        OrganizationAffiliation.class).setParameter("id", id).setMaxResults(1000).getResultList().stream()
                .map(a -> new HistoryView(a.getId(), parent(a.getParent()), a.getValidFrom(), a.getValidUntil())).toList();
    }

    public Mutation create(Create request, Instant now) {
        if (request.type() == null) throw new IllegalArgumentException("Odaberite tip organizacije.");
        if ((request.type() == OrganizationType.MINISTRY) != (request.parentId() == null))
            throw new IllegalArgumentException("Savez i udruženje moraju imati odgovarajuću nadređenu organizaciju.");
        var organization = new Organization(request.name(), request.type(), request.contactEmail(), request.contactPhone());
        OrganizationAffiliation affiliation = request.parentId() == null ? null
                : new OrganizationAffiliation(organization, requireActive(request.parentId()), now, null);
        em.persist(organization);
        if (affiliation != null) em.persist(affiliation);
        em.flush();
        return new Mutation(organization, affiliation, view(organization, now));
    }

    public Mutation update(UUID id, Update request, Instant now) {
        var organization = requireActive(id);
        version(organization, request.version());
        organization.updateContact(request.name(), request.contactEmail(), request.contactPhone());
        em.flush();
        return new Mutation(organization, null, view(organization, now));
    }

    public Mutation changeParent(UUID id, ChangeParent request, Instant now) {
        var organization = em.find(Organization.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (organization == null) throw new EntityNotFoundException("Organizacija nije dostupna.");
        version(organization, request.version());
        if (!organization.isActive()) throw new IllegalArgumentException("Organizacija je arhivirana.");
        var parent = requireActive(request.parentId());
        Instant effective = request.effectiveFrom() == null ? now : request.effectiveFrom();
        if (effective.isBefore(now)) throw new IllegalArgumentException("Promjena ne može djelovati retroaktivno.");
        var open = em.createQuery("select a from OrganizationAffiliation a where a.child.id = :id and a.validUntil is null",
                OrganizationAffiliation.class).setParameter("id", id).getResultList();
        if (!open.isEmpty()) {
            var previous = open.getFirst();
            if (previous.getValidFrom().isAfter(now)) throw new IllegalStateException("Već postoji zakazana promjena.");
            if (previous.getParent().getId().equals(parent.getId())) throw new IllegalArgumentException("Organizacija već pripada odabranom roditelju.");
            previous.endAt(effective);
            em.flush(); // Close the old interval before inserting the new one.
        }
        var affiliation = new OrganizationAffiliation(organization, parent, effective, null);
        em.persist(affiliation);
        em.lock(organization, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        em.flush();
        return new Mutation(organization, affiliation, view(organization, now));
    }

    private Organization require(UUID id) {
        if (id == null) throw new IllegalArgumentException("Organizacija je obavezna.");
        var organization = em.find(Organization.class, id);
        if (organization == null) throw new EntityNotFoundException("Organizacija nije dostupna.");
        return organization;
    }
    private Organization requireActive(UUID id) {
        var organization = require(id);
        if (!organization.isActive()) throw new IllegalArgumentException("Organizacija je arhivirana.");
        return organization;
    }
    private void version(Organization organization, Long expected) {
        if (expected == null || !expected.equals(organization.getVersion()))
            throw new OptimisticLockException("Podaci su promijenjeni. Osvježite pregled.");
    }
    private View view(Organization organization, Instant now) {
        var affiliation = em.createQuery("select a from OrganizationAffiliation a join fetch a.parent where a.child.id = :id"
                + " and a.validFrom <= :now and (a.validUntil is null or a.validUntil > :now)", OrganizationAffiliation.class)
                .setParameter("id", organization.getId()).setParameter("now", now).getResultStream().findFirst();
        return new View(organization.getId(), organization.getVersion(), organization.getName(), organization.getType(),
                organization.getStatus(), organization.getContactEmail(), organization.getContactPhone(),
                affiliation.map(a -> parent(a.getParent())).orElse(null));
    }
    private ParentView parent(Organization organization) { return new ParentView(organization.getId(), organization.getName()); }
}
