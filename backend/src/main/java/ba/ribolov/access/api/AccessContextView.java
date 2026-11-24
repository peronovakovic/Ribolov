package ba.ribolov.access.api;

import java.time.Instant;
import java.util.UUID;

public record AccessContextView(UUID id, Role role, OrganizationView organization, Instant validFrom, Instant validUntil) {
    public record OrganizationView(UUID id, String name) {}
}
