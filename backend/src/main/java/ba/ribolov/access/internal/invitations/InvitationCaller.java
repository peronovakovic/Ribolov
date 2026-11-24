package ba.ribolov.access.internal.invitations;
import ba.ribolov.access.internal.administration.AdminCaller;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
public record InvitationCaller(AdminCaller identity,String email,boolean emailVerified,String displayName) {
    public static InvitationCaller from(OidcUser user) {
        var identity=AdminCaller.from(user); var token=user.getIdToken();
        return new InvitationCaller(identity,token.getEmail(),Boolean.TRUE.equals(token.getEmailVerified()),token.getFullName());
    }
}
