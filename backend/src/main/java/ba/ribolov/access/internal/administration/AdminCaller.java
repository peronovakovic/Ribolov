package ba.ribolov.access.internal.administration;

import java.time.Instant;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public record AdminCaller(String issuer, String subject, String acr, Instant authenticatedAt) {
    public static AdminCaller from(OidcUser user) {
        if (user == null || user.getIdToken() == null) throw new org.springframework.security.access.AccessDeniedException("Prijava je potrebna.");
        var token = user.getIdToken();
        return new AdminCaller(token.getIssuer() == null ? null : token.getIssuer().toString(),
                token.getSubject(), token.getClaimAsString("acr"), token.getAuthenticatedAt());
    }
}
