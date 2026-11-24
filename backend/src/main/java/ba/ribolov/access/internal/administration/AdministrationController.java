package ba.ribolov.access.internal.administration;

import ba.ribolov.organizations.api.OrganizationManagement;
import ba.ribolov.organizations.model.OrganizationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
class AdministrationController {
    private final AdministrationService service;
    AdministrationController(AdministrationService service) { this.service = service; }
    record CreateOrganization(String name, OrganizationType type, String contactEmail, String contactPhone, UUID parentId, String reason) {}
    record UpdateOrganization(Long version, String name, String contactEmail, String contactPhone, String reason) {}
    record ChangeParent(Long version, UUID parentId, Instant effectiveFrom, String reason) {}

    @GetMapping("/organizations")
    ResponseEntity<OrganizationManagement.Page> organizations(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page) {
        return response(service.organizations(AdminCaller.from(user), context, search, page));
    }
    @GetMapping("/organizations/{id}/history")
    ResponseEntity<List<OrganizationManagement.HistoryView>> history(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @PathVariable UUID id) {
        return response(service.history(AdminCaller.from(user), context, id));
    }
    @PostMapping("/organizations")
    ResponseEntity<OrganizationManagement.View> createOrganization(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @RequestBody CreateOrganization request) {
        return created(service.createOrganization(AdminCaller.from(user), context,
                new OrganizationManagement.Create(request.name(), request.type(), request.contactEmail(), request.contactPhone(), request.parentId()), request.reason()));
    }
    @PutMapping("/organizations/{id}")
    ResponseEntity<OrganizationManagement.View> updateOrganization(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @PathVariable UUID id, @RequestBody UpdateOrganization request) {
        return response(service.updateOrganization(AdminCaller.from(user), context, id,
                new OrganizationManagement.Update(request.version(), request.name(), request.contactEmail(), request.contactPhone()), request.reason()));
    }
    @PostMapping("/organizations/{id}/parent")
    ResponseEntity<OrganizationManagement.View> changeParent(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @PathVariable UUID id, @RequestBody ChangeParent request) {
        return response(service.changeParent(AdminCaller.from(user), context, id,
                new OrganizationManagement.ChangeParent(request.version(), request.parentId(), request.effectiveFrom()), request.reason()));
    }
    @GetMapping("/users")
    ResponseEntity<AdminViews.Page<AdminViews.User>> users(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page) {
        return response(service.users(AdminCaller.from(user), context, search, page));
    }
    @PutMapping("/users/{id}")
    ResponseEntity<AdminViews.User> updateUser(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @PathVariable UUID id, @RequestBody AdminViews.UpdateUser request) {
        return response(service.updateUser(AdminCaller.from(user), context, id, request));
    }
    @GetMapping("/roles")
    ResponseEntity<AdminViews.Page<AdminViews.Assignment>> roles(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @RequestParam(defaultValue = "0") int page) {
        return response(service.assignments(AdminCaller.from(user), context, page));
    }
    @PostMapping("/roles/{id}/revoke")
    ResponseEntity<AdminViews.Assignment> revoke(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @PathVariable UUID id, @RequestBody AdminViews.Revoke request) {
        return response(service.revoke(AdminCaller.from(user), context, id, request));
    }
    @GetMapping("/events")
    ResponseEntity<AdminViews.Page<AdminViews.Event>> events(@AuthenticationPrincipal OidcUser user,
            @RequestHeader("X-Access-Context") UUID context, @RequestParam(defaultValue = "0") int page) {
        return response(service.events(AdminCaller.from(user), context, page));
    }
    private <T> ResponseEntity<T> response(T body) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body); }
    private <T> ResponseEntity<T> created(T body) { return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(body); }
}
