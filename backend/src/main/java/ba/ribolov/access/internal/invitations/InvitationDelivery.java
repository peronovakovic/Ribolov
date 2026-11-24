package ba.ribolov.access.internal.invitations;
import ba.ribolov.access.internal.domain.RoleInvitation;
import ba.ribolov.notifications.api.EmailDelivery;
import jakarta.persistence.*;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationDelivery {
    private final EntityManager em; private final InvitationTokens tokens; private final EmailDelivery mail;
    private final Clock clock; private final boolean databaseContext; private final String webUrl;
    public InvitationDelivery(EntityManager em,InvitationTokens tokens,EmailDelivery mail,Clock clock,
        @Value("${ribolov.database.identity-context:true}") boolean databaseContext,@Value("${ribolov.web-url}") String webUrl) {
        this.em=em;this.tokens=tokens;this.mail=mail;this.clock=clock;this.databaseContext=databaseContext;this.webUrl=webUrl;
        var uri=java.net.URI.create(webUrl);
        if(uri.getFragment()!=null || uri.getQuery()!=null || uri.getHost()==null)
            throw new IllegalArgumentException("WEB_URL must be an absolute URL without query or fragment.");
    }
    /** Bounded SMTP timeout; row lock prevents simultaneous deliveries. A crash after SMTP may resend the same link. */
    @Transactional
    public boolean deliverNext() {
        if(databaseContext)em.createNativeQuery("select set_config('ribolov.invitation_worker','true',true)").getSingleResult();
        var now=clock.instant();
        var rows=em.createQuery("select i from RoleInvitation i where i.acceptedAt is null and i.revokedAt is null"
            +" and i.sentAt is null and i.expiresAt>:now and i.nextAttemptAt<=:now and i.attempts<5 order by i.createdAt,i.id",RoleInvitation.class)
            .setParameter("now",now).setMaxResults(1).setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setHint("jakarta.persistence.lock.timeout",-2).getResultList();
        if(rows.isEmpty())return false;
        var i=rows.getFirst();
        if(databaseContext)em.createNativeQuery("select set_config('ribolov.delivery_invitation_id',:id,true)")
            .setParameter("id",i.getId().toString()).getSingleResult();
        try {
            if(!i.authorizesAt(now))throw new IllegalStateException("Invitation authority withdrawn");
            var token=tokens.token(i.getId());
            if(!InvitationTokens.hash(token).equals(i.getTokenHash()))throw new IllegalStateException("Invitation key changed");
            mail.send(i.getEmail(),"Poziv za pristup portalu Ribolov",
                "Pozvani ste da preuzmete poslovnu ulogu u portalu Ribolov.\n\n"
                +"Otvorite vezu, prijavite se ili napravite nalog s ovom adresom e-pošte, potvrdite adresu i uključite drugi faktor prijave.\n"
                +"Prije prihvatanja prikazat ćemo organizaciju i ulogu. Poziv važi do "+i.getExpiresAt()+" (UTC).\n\n"
                +webUrl+"#invite="+token+"\n\nAko niste očekivali poziv, zanemarite ovu poruku.");
            i.delivered(now);
        } catch(RuntimeException failure) {i.failed(now);}
        em.flush();return true;
    }
}
