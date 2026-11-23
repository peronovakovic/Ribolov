package ba.ribolov.access.internal;

import ba.ribolov.access.api.SessionView;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SessionController {
    @GetMapping("/api/v1/session")
    ResponseEntity<SessionView> webSession(@AuthenticationPrincipal OidcUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new SessionView(user.getSubject(), user.getFullName()));
    }

    @GetMapping("/api/mobile/v1/session")
    ResponseEntity<SessionView> mobileSession(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new SessionView(jwt.getSubject(), jwt.getClaimAsString("name")));
    }

    @GetMapping("/api/v1/csrf")
    ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("parameterName", token.getParameterName(), "token", token.getToken()));
    }
}
