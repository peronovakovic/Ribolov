package ba.ribolov.access.internal.administration;

import ba.ribolov.access.api.Role;
import ba.ribolov.organizations.api.OrganizationManagement.ParentView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AdminViews {
    private AdminViews() {}
    public record User(UUID id, Long version, String subject, String displayName, boolean active) {}
    public record UserSummary(UUID id, String displayName) {}
    public record Assignment(UUID id, Long version, UserSummary user, ParentView organization, Role role,
                             Instant validFrom, Instant validUntil, Instant revokedAt) {}
    public record Event(UUID id, Instant occurredAt, String action, String actor, String organization,
                        String targetUser, Role role, String reason) {}
    public record Page<T>(List<T> items, int page, boolean hasNext) {}
    public record CreateUser(String subject, String displayName, String reason) {}
    public record UpdateUser(Long version, String displayName, boolean active, String reason) {}
    public record Grant(UUID userId, UUID organizationId, Role role, Instant validFrom, Instant validUntil, String reason) {}
    public record Revoke(Long version, String reason) {}
}
