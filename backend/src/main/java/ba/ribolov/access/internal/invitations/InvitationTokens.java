package ba.ribolov.access.internal.invitations;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** HMAC-derived, unguessable links; only a SHA-256 digest is stored in the database. */
@Component
public class InvitationTokens {
    private final byte[] key;
    public InvitationTokens(@Value("${ribolov.invitations.signing-key:}") String encoded) {
        key=encoded.isBlank()?new byte[0]:Base64.getDecoder().decode(encoded);
        if(key.length!=0 && key.length!=32) throw new IllegalArgumentException("Invitation signing key must contain 32 bytes.");
    }
    public String token(UUID id) {
        if(key.length==0) throw new IllegalStateException("Slanje poziva nije podešeno.");
        try {
            var mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key,"HmacSHA256"));
            return id+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("ribolov-invitation-v1:"+id).getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.GeneralSecurityException e) {throw new IllegalStateException("Invitation cryptography unavailable");}
    }
    public static String hash(String token) {
        if(token==null || !token.matches("[0-9a-f-]{36}\\.[A-Za-z0-9_-]{43}")) throw new IllegalArgumentException("Poziv nije ispravan.");
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.GeneralSecurityException e){throw new IllegalStateException("Invitation cryptography unavailable");}
    }
}
