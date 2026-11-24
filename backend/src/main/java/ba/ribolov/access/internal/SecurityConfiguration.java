package ba.ribolov.access.internal;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {
    @Bean
    ClientRegistrationRepository registrations(@Value("${ribolov.security.issuer}") String issuer,
                                                @Value("${ribolov.security.web-client-secret}") String secret) {
        // Explicit provider endpoints allow startup without a network discovery request.
        var registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("ribolov-web").clientSecret(secret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email").issuerUri(issuer)
                .authorizationUri(issuer + "/protocol/openid-connect/auth")
                .tokenUri(issuer + "/protocol/openid-connect/token")
                .jwkSetUri(issuer + "/protocol/openid-connect/certs")
                .userInfoUri(issuer + "/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", issuer + "/protocol/openid-connect/logout"))
                .clientName("Ribolov").build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${ribolov.security.issuer}") String issuer) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/protocol/openid-connect/certs").build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), new AudienceValidator("ribolov-api")));
        return decoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain mobileSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/mobile/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable()) // Only the bearer-token chain is exempt from CSRF.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/mobile/v1/session", "/api/mobile/v1/access-contexts").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    SecurityFilterChain webSecurity(HttpSecurity http, ClientRegistrationRepository registrations,
                                   @Value("${ribolov.web-url}") String webUrl) throws Exception {
        var authorizationRequests = new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(
                registrations, "/oauth2/authorization");
        authorizationRequests.setAuthorizationRequestCustomizer(builder -> builder.additionalParameters(parameters -> {
            parameters.put("acr_values", "2");
            parameters.put("claims", "{\"id_token\":{\"acr\":{\"essential\":true,\"values\":[\"2\"]}}}");
            parameters.put("max_age", 300);
        }));
        var loginSuccess = new SimpleUrlAuthenticationSuccessHandler(webUrl);
        loginSuccess.setAlwaysUseDefaultTargetUrl(true);
        var logoutSuccess = new OidcClientInitiatedLogoutSuccessHandler(registrations);
        logoutSuccess.setPostLogoutRedirectUri(webUrl);
        return http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/liveness",
                                "/actuator/health/readiness", "/api/v1/csrf").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/session", "/api/v1/access-contexts", "/api/v1/access-contexts/{id}").authenticated()
                        .requestMatchers("/api/v1/admin/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/invitations/preview", "/api/v1/invitations/accept").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getServletPath().startsWith("/api/")))
                .oauth2Login(login -> login.successHandler(loginSuccess)
                        .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(authorizationRequests)))
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccess))
                .build();
    }
}