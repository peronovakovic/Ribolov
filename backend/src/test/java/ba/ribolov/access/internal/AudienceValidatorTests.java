package ba.ribolov.access.internal;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTests {
    private final AudienceValidator validator = new AudienceValidator("ribolov-api");

    @Test
    void rejectsTokenIssuedForAnotherApplication() {
        assertThat(validator.validate(token(List.of("another-api"))).hasErrors()).isTrue();
        assertThat(validator.validate(token(List.of())).hasErrors()).isTrue();
    }

    @Test
    void acceptsExpectedAudienceAmongSeveralAudiences() {
        assertThat(validator.validate(token(List.of("account", "ribolov-api"))).hasErrors()).isFalse();
    }

    @Test
    void rejectsTokenWithoutAudienceClaim() {
        var jwt = Jwt.withTokenValue("test-token").header("alg", "RS256").subject("test-user").build();
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private Jwt token(List<String> audiences) {
        return Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject("test-user").claim("aud", audiences).build();
    }
}
