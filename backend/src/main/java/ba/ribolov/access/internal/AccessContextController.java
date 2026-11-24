package ba.ribolov.access.internal;

import ba.ribolov.access.api.AccessContextView;
import ba.ribolov.access.internal.application.AccessContextService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AccessContextController {
    private final AccessContextService contexts;

    AccessContextController(AccessContextService contexts) { this.contexts = contexts; }

    @GetMapping("/api/v1/access-contexts")
    ResponseEntity<List<AccessContextView>> webContexts(@AuthenticationPrincipal OidcUser user) {
        return response(contexts.available(user.getIssuer() == null ? null : user.getIssuer().toString(), user.getSubject()));
    }

    @GetMapping("/api/v1/access-contexts/{id}")
    ResponseEntity<AccessContextView> webContext(@AuthenticationPrincipal OidcUser user, @PathVariable UUID id) {
        return response(contexts.require(user.getIssuer() == null ? null : user.getIssuer().toString(), user.getSubject(), id));
    }

    @GetMapping("/api/mobile/v1/access-contexts")
    ResponseEntity<List<AccessContextView>> mobileContexts(@AuthenticationPrincipal Jwt jwt) {
        return response(contexts.available(jwt.getIssuer() == null ? null : jwt.getIssuer().toString(), jwt.getSubject()));
    }

    private <T> ResponseEntity<T> response(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
