package ba.ribolov.access.internal;

import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.administration.*;
import ba.ribolov.access.internal.domain.*;
import ba.ribolov.access.internal.invitations.*;
import ba.ribolov.notifications.api.EmailDelivery;
import ba.ribolov.organizations.model.*;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.*;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest @ActiveProfiles("test") @Transactional
class InvitationTests {
    private static final Instant START=Instant.parse("2026-11-23T18:00:00Z");
    private static final String ISSUER="https://identity.example.test";
    @Autowired InvitationService service; @Autowired InvitationDelivery delivery; @Autowired InvitationTokens tokens; @Autowired EntityManager em;
    @MockitoBean Clock clock; @MockitoBean EmailDelivery mail;
    private Instant now; private Organization org; private RoleAssignment context;
    @BeforeEach void fixture(){
        now=START;when(clock.instant()).thenAnswer(i->now);
        org=new Organization("Udruženje A",OrganizationType.ASSOCIATION,null,null);em.persist(org);
        var admin=new ApplicationUser(ISSUER,"admin");em.persist(admin);
        context=new RoleAssignment(admin,Role.ASSOCIATION_ADMIN,org,START.minusSeconds(3600),null);em.persist(context);em.flush();
    }
    @Test void acceptanceCreatesIdentityGrantAndAuditAtomicallyAndIsSingleUse(){
        var invitation=create();String token=tokens.token(invitation.id());
        assertThat(invitation.expiresAt()).isEqualTo(START.plus(Duration.ofHours(48)));
        assertThat(service.preview(recipient("member@example.test",true),token).organization()).isEqualTo("Udruženje A");
        var result=service.accept(recipient("MEMBER@example.test",true),token);
        var assignment=em.find(RoleAssignment.class,result.assignmentId());
        assertThat(assignment.getUser().getSubject()).isEqualTo("recipient");
        assertThat(assignment.getOrganization()).isSameAs(org);
        assertThatThrownBy(()->service.accept(recipient("member@example.test",true),token)).isInstanceOf(IllegalStateException.class);
        assertThat(em.createQuery("select count(e) from AdministrationEvent e where e.invitation.id=:id",Long.class).setParameter("id",invitation.id()).getSingleResult()).isEqualTo(2);
        assertThat(em.find(RoleInvitation.class,invitation.id()).getTokenHash()).doesNotContain(token);
    }
    @Test void anotherEmailUnverifiedEmailAndStaleMfaCannotAccept(){
        var i=create();var token=tokens.token(i.id());
        assertThatThrownBy(()->service.accept(recipient("other@example.test",true),token)).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        assertThatThrownBy(()->service.accept(recipient("member@example.test",false),token)).isInstanceOf(AccessDeniedException.class);
        var stale=new InvitationCaller(new AdminCaller(ISSUER,"recipient","2",now.minusSeconds(301)),"member@example.test",true,"Member");
        assertThatThrownBy(()->service.accept(stale,token)).isInstanceOf(ReauthenticationRequired.class);
        assertThat(em.find(RoleInvitation.class,i.id()).getAcceptedAssignment()).isNull();
    }
    @Test void expiredRevokedAndWithdrawnAuthorityAreRejected(){
        var expired=create();now=START.plus(Duration.ofHours(48));
        assertThatThrownBy(()->service.accept(recipient("member@example.test",true),tokens.token(expired.id()))).isInstanceOf(IllegalStateException.class);
        now=START;var revoked=create();service.revoke(admin(),context.getId(),revoked.id(),new InvitationService.Change(revoked.version(),"Povlačenje"));
        assertThatThrownBy(()->service.accept(recipient("member@example.test",true),tokens.token(revoked.id()))).isInstanceOf(IllegalStateException.class);
        var withdrawn=create();context.revoke(now);
        assertThatThrownBy(()->service.accept(recipient("member@example.test",true),tokens.token(withdrawn.id()))).isInstanceOf(IllegalStateException.class);
    }
    @Test void inactiveLocalUserCannotBeReactivatedByInvitation(){
        var inactive=new ApplicationUser(ISSUER,"recipient");inactive.updateProfile("Inactive",false);em.persist(inactive);em.flush();
        var i=create();
        assertThatThrownBy(()->service.accept(recipient("member@example.test",true),tokens.token(i.id()))).isInstanceOf(AccessDeniedException.class);
        assertThat(inactive.isActive()).isFalse();assertThat(em.find(RoleInvitation.class,i.id()).getAcceptedAssignment()).isNull();
    }
    @Test void invitationsCannotEscalateOrCrossOrganizations(){
        var foreign=new Organization("B",OrganizationType.ASSOCIATION,null,null);em.persist(foreign);em.flush();
        assertThatThrownBy(()->service.create(admin(),context.getId(),new InvitationService.Create("member@example.test",Role.OPERATOR,foreign.getId(),null,"Test"))).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(()->service.create(admin(),context.getId(),new InvitationService.Create("member@example.test",Role.ASSOCIATION_ADMIN,org.getId(),null,"Test"))).isInstanceOf(AccessDeniedException.class);
    }
    @Test void deliveryRetriesSameLinkWithoutGrantingRoleOrExtendingExpiry(){
        var i=create();String token=tokens.token(i.id());
        doThrow(new IllegalStateException("SMTP failure")).doNothing().when(mail).send(anyString(),anyString(),anyString());
        assertThat(delivery.deliverNext()).isTrue();
        var entity=em.find(RoleInvitation.class,i.id());assertThat(entity.getAttempts()).isEqualTo(1);assertThat(entity.getSentAt()).isNull();
        now=START.plusSeconds(180);assertThat(delivery.deliverNext()).isTrue();assertThat(entity.getSentAt()).isEqualTo(now);
        verify(mail,times(2)).send(eq("member@example.test"),anyString(),contains("#invite="+token));
        assertThat(entity.getAcceptedAssignment()).isNull();assertThat(entity.getExpiresAt()).isEqualTo(i.expiresAt());
        now=now.plusSeconds(61);
        service.retry(admin(),context.getId(),i.id(),new InvitationService.Change(entity.getVersion(),"Ponovno slanje"));
        assertThat(entity.getSentAt()).isNull();assertThat(entity.getExpiresAt()).isEqualTo(i.expiresAt());
    }
    @Test void revokedInvitationIsNeverSent(){
        var i=create();service.revoke(admin(),context.getId(),i.id(),new InvitationService.Change(i.version(),"Povlačenje"));
        assertThat(delivery.deliverNext()).isFalse();verifyNoInteractions(mail);
    }
    @Test void recipientClaimsComeFromIdTokenEvenIfUserinfoDiffers(){
        var token=new OidcIdToken("test",START,START.plusSeconds(3600),Map.of("iss",ISSUER,"sub","recipient","acr","2","auth_time",START.getEpochSecond(),"email","id@example.test","email_verified",false));
        var info=new OidcUserInfo(Map.of("sub","recipient","email","userinfo@example.test","email_verified",true));
        var user=new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")),token,info);
        var caller=InvitationCaller.from(user);assertThat(caller.email()).isEqualTo("id@example.test");assertThat(caller.emailVerified()).isFalse();
    }
    private InvitationService.View create(){return service.create(admin(),context.getId(),new InvitationService.Create("Member@example.test",Role.OPERATOR,org.getId(),null,"Novo zaduženje"));}
    private AdminCaller admin(){return new AdminCaller(ISSUER,"admin","2",now);}
    private InvitationCaller recipient(String email,boolean verified){return new InvitationCaller(new AdminCaller(ISSUER,"recipient","2",now),email,verified,"Novi član");}
}
