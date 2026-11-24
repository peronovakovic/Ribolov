package ba.ribolov.organizations.api;

import ba.ribolov.organizations.model.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrganizationManagement {
    record View(UUID id, Long version, String name, OrganizationType type, OrganizationStatus status,
                String contactEmail, String contactPhone, ParentView parent) {}
    record ParentView(UUID id, String name) {}
    record HistoryView(UUID id, ParentView parent, Instant validFrom, Instant validUntil) {}
    record Page(List<View> items, int page, boolean hasNext) {}
    record Create(String name, OrganizationType type, String contactEmail, String contactPhone, UUID parentId) {}
    record Update(Long version, String name, String contactEmail, String contactPhone) {}
    record ChangeParent(Long version, UUID parentId, Instant effectiveFrom) {}
    record Mutation(Organization organization, OrganizationAffiliation affiliation, View view) {}

    Page list(UUID organizationScope, String search, int page, Instant now);
    List<HistoryView> history(UUID id);
    Mutation create(Create request, Instant now);
    Mutation update(UUID id, Update request, Instant now);
    Mutation changeParent(UUID id, ChangeParent request, Instant now);
}
