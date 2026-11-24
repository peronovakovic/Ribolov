package ba.ribolov.access.internal;
import ba.ribolov.access.api.Role;
import ba.ribolov.access.internal.administration.AdminCaller;
import ba.ribolov.access.internal.invitations.*;
import ba.ribolov.notifications.api.EmailDelivery;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties={
 "spring.datasource.url=${IT_DATABASE_URL}","spring.datasource.username=${IT_DATABASE_USERNAME}","spring.datasource.password=${IT_DATABASE_PASSWORD}",
 "spring.flyway.enabled=true","spring.flyway.url=${IT_DATABASE_URL}","spring.flyway.user=${IT_MIGRATION_USERNAME}","spring.flyway.password=${IT_MIGRATION_PASSWORD}",
 "spring.jpa.hibernate.ddl-auto=validate","ribolov.database.identity-context=true"})
@ActiveProfiles("test")
class PostgresInvitationIT {
 private static final String ISSUER="https://identity.example.test";
 @Autowired InvitationService service;@Autowired InvitationDelivery delivery;@Autowired InvitationTokens tokens;@Autowired DataSource runtime;
 @MockitoBean EmailDelivery mail;
 private Connection owner;private UUID sys,admin,a,b,sysContext,adminContext;private String subject;
 @BeforeEach void fixtures() throws Exception {
  owner=DriverManager.getConnection(System.getenv("IT_DATABASE_URL"),System.getenv("IT_MIGRATION_USERNAME"),System.getenv("IT_MIGRATION_PASSWORD"));
  sys=UUID.randomUUID();admin=UUID.randomUUID();a=UUID.randomUUID();b=UUID.randomUUID();sysContext=UUID.randomUUID();adminContext=UUID.randomUUID();subject="recipient-"+UUID.randomUUID();
  for(UUID id:List.of(sys,admin))exec("INSERT INTO access.application_user(id,issuer,subject,display_name) VALUES (?,?,?,'Admin')",id,ISSUER,id.toString());
  for(UUID id:List.of(a,b))exec("INSERT INTO organizations.organization(id,name,type,status) VALUES (?,'Udruzenje','ASSOCIATION','ACTIVE')",id);
  exec("INSERT INTO access.role_assignment(id,user_id,role,valid_from) VALUES (?,?,'SYSTEM_ADMIN',CURRENT_TIMESTAMP-interval '1 hour')",sysContext,sys);
  exec("INSERT INTO access.role_assignment(id,user_id,organization_id,role,valid_from) VALUES (?,?,?,'ASSOCIATION_ADMIN',CURRENT_TIMESTAMP-interval '1 hour')",adminContext,admin,a);
 }
 @AfterEach void cleanup() throws Exception {
  if(owner==null)return;
  try {
   for(UUID org:List.of(a,b)){
    exec("DELETE FROM access.administration_event WHERE organization_id=?",org);
    exec("DELETE FROM access.role_invitation WHERE organization_id=?",org);
    exec("DELETE FROM access.role_assignment WHERE organization_id=?",org);
    exec("DELETE FROM organizations.organization WHERE id=?",org);
   }
   exec("DELETE FROM access.role_assignment WHERE user_id=?",sys);
   for(String sub:List.of(sys.toString(),admin.toString(),subject))exec("DELETE FROM access.application_user WHERE issuer=? AND subject=?",ISSUER,sub);
  } finally {owner.close();}
 }
 @Test void acceptanceWithRuntimeAccountCreatesOnlyInvitedRoleAndCanReadAudit() throws Exception {
  var i=create(admin,adminContext,a,Role.OPERATOR);var result=service.accept(recipient(),tokens.token(i.id()));
  assertThat(result.role()).isEqualTo(Role.OPERATOR);
  assertThat(service.list(caller(admin),adminContext,0).items()).singleElement().satisfies(v->assertThat(v.status()).isEqualTo("ACCEPTED"));
  assertThat(count("SELECT count(*) FROM access.administration_event WHERE organization_id='"+a+"'")).isEqualTo(2);
  assertThat(count("SELECT count(*) FROM access.role_assignment r JOIN access.application_user u ON u.id=r.user_id WHERE u.subject='"+subject+"'")).isEqualTo(1);
  assertThatThrownBy(()->service.accept(recipient(),tokens.token(i.id()))).isInstanceOf(IllegalStateException.class);
 }
 @Test void systemInvitationAndBackgroundDeliveryUseRestrictedRelations() {
  var i=create(sys,sysContext,a,Role.ASSOCIATION_ADMIN);
  assertThat(delivery.deliverNext()).isTrue();verify(mail).send(eq("member@example.test"),anyString(),contains("#invite="+tokens.token(i.id())));
  var result=service.accept(recipient(),tokens.token(i.id()));assertThat(result.role()).isEqualTo(Role.ASSOCIATION_ADMIN);
 }
 @Test void wrongEmailExpiredInvitationAndWithdrawnAuthorCannotGrant() throws Exception {
  var i=create(admin,adminContext,a,Role.OPERATOR);String token=tokens.token(i.id());
  var wrong=new InvitationCaller(recipient().identity(),"wrong@example.test",true,"Recipient");
  assertThatThrownBy(()->service.accept(wrong,token)).isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
  exec("UPDATE access.role_assignment SET revoked_at=CURRENT_TIMESTAMP WHERE id=?",adminContext);
  assertThatThrownBy(()->service.accept(recipient(),token)).isInstanceOf(IllegalStateException.class);
  exec("UPDATE access.role_assignment SET revoked_at=NULL WHERE id=?",adminContext);
  exec("UPDATE access.role_invitation SET created_at=CURRENT_TIMESTAMP-interval '49 hours',expires_at=CURRENT_TIMESTAMP-interval '1 hour' WHERE id=?",i.id());
  assertThatThrownBy(()->service.accept(recipient(),token)).isInstanceOf(IllegalStateException.class);
  assertThat(count("SELECT count(*) FROM access.application_user WHERE subject='"+subject+"'")).isZero();
 }
 @Test void onlyOneConcurrentAcceptanceSucceeds() throws Exception {
  var i=create(admin,adminContext,a,Role.OPERATOR);String token=tokens.token(i.id());
  var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){
   Callable<Boolean> accept=()->{start.await();try{service.accept(recipient(),token);return true;}catch(IllegalStateException e){return false;}};
   var one=pool.submit(accept);var two=pool.submit(accept);start.countDown();
   assertThat(List.of(one.get(20,TimeUnit.SECONDS),two.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
  }
  assertThat(count("SELECT count(*) FROM access.administration_event WHERE action='INVITATION_ACCEPTED' AND organization_id='"+a+"'")).isEqualTo(1);
 }
 @Test void failedAuditRollsBackInvitationAndNoMailIsSent() throws Exception {
  assertThatThrownBy(()->service.create(caller(admin),adminContext,new InvitationService.Create("member@example.test",Role.OPERATOR,a,null," "))).isInstanceOf(IllegalArgumentException.class);
  assertThat(count("SELECT count(*) FROM access.role_invitation WHERE organization_id='"+a+"'")).isZero();
  assertThat(delivery.deliverNext()).isFalse();verifyNoInteractions(mail);
 }
 @Test void rowPoliciesHideTokensAndRejectUnauthorizedStateChanges() throws Exception {
  var i=create(admin,adminContext,a,Role.OPERATOR);
  try(var c=runtime.getConnection()){
   c.setAutoCommit(false);
   try(var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM access.role_invitation")){rows.next();assertThat(rows.getInt(1)).isZero();}
   try(var q=c.createStatement()){
    assertThat(q.executeUpdate("UPDATE access.role_invitation SET revoked_at=CURRENT_TIMESTAMP WHERE id='"+i.id()+"'")).isZero();
   }
   c.rollback();
   try(var q=c.prepareStatement("SELECT set_config('ribolov.invitation_worker','true',true)")){q.execute();}
   try(var q=c.createStatement()){
    assertThatThrownBy(()->q.executeUpdate("UPDATE access.role_invitation SET revoked_at=CURRENT_TIMESTAMP WHERE id='"+i.id()+"'")).isInstanceOf(SQLException.class)
      .satisfies(e->assertThat(((SQLException)e).getSQLState()).isEqualTo("42501"));
   }
   c.rollback();
   try(var q=c.createStatement();var rows=q.executeQuery("SELECT count(*) FROM access.role_invitation")){rows.next();assertThat(rows.getInt(1)).isZero();}
   c.rollback();
  }
 }
 private InvitationService.View create(UUID user,UUID context,UUID org,Role role){return service.create(caller(user),context,new InvitationService.Create("member@example.test",role,org,null,"Imenovanje"));}
 private AdminCaller caller(UUID user){return new AdminCaller(ISSUER,user.toString(),"2",Instant.now());}
 private InvitationCaller recipient(){return new InvitationCaller(new AdminCaller(ISSUER,subject,"2",Instant.now()),"member@example.test",true,"Novi korisnik");}
 private void exec(String sql,Object... args)throws SQLException{try(var q=owner.prepareStatement(sql)){for(int i=0;i<args.length;i++)q.setObject(i+1,args[i]);q.executeUpdate();}}
 private int count(String sql)throws SQLException{try(var q=owner.createStatement();var r=q.executeQuery(sql)){r.next();return r.getInt(1);}}
}
