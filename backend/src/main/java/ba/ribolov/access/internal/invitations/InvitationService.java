package ba.ribolov.access.internal.invitations;

import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.administration.*;
import ba.ribolov.access.internal.domain.*;
import ba.ribolov.organizations.model.Organization;
import jakarta.persistence.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class InvitationService {
    public record Create(String email, Role role, UUID organizationId, Instant validUntil, String reason) {}
    public record Change(Long version,String reason) {}
    public record View(UUID id,Long version,String email,Role role,UUID organizationId,String organization,
        Instant createdAt,Instant expiresAt,Instant validUntil,String status,Instant sentAt,int attempts,String deliveryFailure) {}
    public record Preview(Role role,String organization,Instant expiresAt,Instant validUntil) {}
    public record Accepted(UUID assignmentId,Role role,String organization) {}
    private final EntityManager em; private final AdminAuthorization authorization; private final InvitationTokens tokens;
    private final Clock clock; private final boolean databaseContext;
    public InvitationService(EntityManager em,AdminAuthorization authorization,InvitationTokens tokens,Clock clock,
        @Value("${ribolov.database.identity-context:true}") boolean databaseContext) {
        this.em=em;this.authorization=authorization;this.tokens=tokens;this.clock=clock;this.databaseContext=databaseContext;
    }
    @Transactional(readOnly=true)
    public AdminViews.Page<View> list(AdminCaller caller,UUID contextId,int page) {
        var context=authorization.require(caller,contextId,false);
        if(page<0 || page>10000) throw new IllegalArgumentException("Neispravna stranica.");
        var q=em.createQuery("select i from RoleInvitation i join fetch i.organization o"
            +(context.getRole()==Role.SYSTEM_ADMIN?"":" where o.id=:org")+" order by i.createdAt desc,i.id",RoleInvitation.class)
            .setFirstResult(page*50).setMaxResults(51);
        if(context.getRole()!=Role.SYSTEM_ADMIN)q.setParameter("org",context.getOrganization().getId());
        var rows=q.getResultList();return new AdminViews.Page<>(rows.stream().limit(50).map(this::view).toList(),page,rows.size()>50);
    }
    public View create(AdminCaller caller,UUID contextId,Create request) {
        var context=authorization.require(caller,contextId,true); authorization.role(context,request.role(),request.organizationId());
        var org=request.organizationId()==null?null:em.find(Organization.class,request.organizationId());
        if(org==null)throw new EntityNotFoundException("Organizacija nije dostupna.");
        var id=UUID.randomUUID();var now=now();
        var invitation=new RoleInvitation(id,request.email(),request.role(),org,context,InvitationTokens.hash(tokens.token(id)),now,request.validUntil());
        em.persist(invitation);em.flush();audit(context.getUser(),invitation,null,AdministrationEvent.Action.INVITATION_CREATED,request.reason());
        return view(invitation);
    }
    public View revoke(AdminCaller caller,UUID contextId,UUID id,Change request) {
        var context=authorization.require(caller,contextId,true);var invitation=editable(context,id,request.version());
        invitation.revoke(now());em.flush();audit(context.getUser(),invitation,null,AdministrationEvent.Action.INVITATION_REVOKED,request.reason());
        return view(invitation);
    }
    public View retry(AdminCaller caller,UUID contextId,UUID id,Change request) {
        var context=authorization.require(caller,contextId,true);var invitation=editable(context,id,request.version());
        // Resend the same link, preserving the original expiry and single-use guarantee.
        if(invitation.getSentAt()!=null && invitation.getSentAt().isAfter(now().minusSeconds(60)))
            throw new IllegalStateException("Pričekajte jednu minutu prije ponovnog slanja.");
        invitation.retry(now());em.flush();audit(context.getUser(),invitation,null,AdministrationEvent.Action.INVITATION_RESENT,request.reason());
        return view(invitation);
    }
    private RoleInvitation editable(RoleAssignment context,UUID id,Long version) {
        var i=em.find(RoleInvitation.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(i==null)throw new EntityNotFoundException("Poziv nije dostupan.");
        authorization.role(context,i.getRole(),i.getOrganization().getId());
        if(!Objects.equals(i.getVersion(),version))throw new OptimisticLockException();
        return i;
    }
    @Transactional(readOnly=true)
    public Preview preview(InvitationCaller caller,String token) {
        String email=recipientContext(caller,token);
        var i=invitation(token,false);
        requirePending(i,email);
        return new Preview(i.getRole(),i.getOrganization().getName(),i.getExpiresAt(),i.getValidUntil());
    }
    public Accepted accept(InvitationCaller caller,String token) {
        String email=recipientContext(caller,token);
        var i=invitation(token,true);requirePending(i,email);
        // Serialize creation of the same issuer/subject across different invitations.
        if(databaseContext) em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:identity, 0))")
            .setParameter("identity",caller.identity().issuer()+"\n"+caller.identity().subject()).getSingleResult();
        var matches=em.createQuery("select u from ApplicationUser u where u.issuer=:issuer and u.subject=:subject",ApplicationUser.class)
            .setParameter("issuer",caller.identity().issuer()).setParameter("subject",caller.identity().subject()).getResultList();
        ApplicationUser user;
        if(matches.isEmpty()) {
            user=new ApplicationUser(caller.identity().issuer(),caller.identity().subject());
            String name=caller.displayName()==null || caller.displayName().isBlank()?caller.email():caller.displayName();
            user.updateProfile(name,true);em.persist(user);em.flush();
        } else {user=matches.getFirst();if(!user.isActive())throw new AccessDeniedException("Nalog nije aktivan.");}
        var assignment=new RoleAssignment(user,i.getRole(),i.getOrganization(),now(),i.getValidUntil());
        em.persist(assignment);em.flush();i.accept(assignment,now());em.flush();
        audit(user,i,assignment,AdministrationEvent.Action.INVITATION_ACCEPTED,"Prihvatanje jednokratnog poziva uz potvrđenu adresu i MFA");
        return new Accepted(assignment.getId(),assignment.getRole(),i.getOrganization().getName());
    }
    private String recipientContext(InvitationCaller caller,String token) {
        var identity=caller.identity();var now=now();
        if(!"2".equals(identity.acr()) || identity.authenticatedAt()==null
            ||identity.authenticatedAt().isBefore(now.minusSeconds(300))||identity.authenticatedAt().isAfter(now.plusSeconds(30)))
            throw new ReauthenticationRequired();
        if(!caller.emailVerified())throw new AccessDeniedException("Prvo potvrdite adresu e-pošte u sistemu za prijavu.");
        String email=RoleInvitation.normalizeEmail(caller.email());
        String hash=InvitationTokens.hash(token);
        if(identity.issuer()==null || identity.subject()==null)throw new AccessDeniedException("Identitet nije potvrđen.");
        if(databaseContext) em.createNativeQuery("""
            select set_config('ribolov.issuer',:issuer,true),set_config('ribolov.subject',:subject,true),
                   set_config('ribolov.invitation_hash',:hash,true),set_config('ribolov.verified_email',:email,true),
                   set_config('ribolov.acr','2',true),set_config('ribolov.auth_time',:time,true),
                   set_config('ribolov.assignment_id','',true),set_config('ribolov.invitation_worker','',true)
            """).setParameter("issuer",identity.issuer()).setParameter("subject",identity.subject()).setParameter("hash",hash)
            .setParameter("email",email).setParameter("time",Long.toString(identity.authenticatedAt().getEpochSecond())).getSingleResult();
        // Also retain an explicit application check when running without PostgreSQL policies.
        return email;
    }
    // Request state is passed through the transaction, never through a shared service field.
    private RoleInvitation invitation(String token,boolean lock) {
        var q=em.createQuery("select i from RoleInvitation i where i.tokenHash=:hash",RoleInvitation.class).setParameter("hash",InvitationTokens.hash(token));
        if(lock)q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        var rows=q.getResultList();
        if(rows.isEmpty())throw new EntityNotFoundException("Poziv nije dostupan.");
        return rows.getFirst();
    }
    private void requirePending(RoleInvitation i,String email) {
        if(!i.getEmail().equals(email))throw new EntityNotFoundException("Poziv nije dostupan.");
        if(!i.pendingAt(now()) || !i.authorizesAt(now()))throw new IllegalStateException("Poziv je istekao, opozvan, iskorišten ili pošiljalac više nema ovlaštenje.");
    }
    private void audit(ApplicationUser actor,RoleInvitation invitation,RoleAssignment assignment,AdministrationEvent.Action action,String reason) {
        var e=new AdministrationEvent(actor,invitation.getOrganization(),assignment==null?null:assignment.getUser(),assignment,null,action,reason,now());
        e.linkInvitation(invitation);em.persist(e);em.flush();
    }
    private View view(RoleInvitation i) {return new View(i.getId(),i.getVersion(),i.getEmail(),i.getRole(),i.getOrganization().getId(),
        i.getOrganization().getName(),i.getCreatedAt(),i.getExpiresAt(),i.getValidUntil(),i.status(now()),i.getSentAt(),i.getAttempts(),i.getDeliveryFailure());}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
}
