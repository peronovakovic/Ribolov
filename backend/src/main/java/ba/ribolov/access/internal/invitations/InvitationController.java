package ba.ribolov.access.internal.invitations;
import ba.ribolov.access.internal.administration.AdminCaller;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class InvitationController {
    private final InvitationService service;
    public InvitationController(InvitationService service){this.service=service;}
    public record TokenRequest(String token){}
    @GetMapping("/api/v1/admin/invitations")
    public ResponseEntity<?> list(@AuthenticationPrincipal OidcUser user,@RequestHeader("X-Access-Context") UUID context,@RequestParam(defaultValue="0") int page){
        return response(service.list(AdminCaller.from(user),context,page),200);
    }
    @PostMapping("/api/v1/admin/invitations")
    public ResponseEntity<?> create(@AuthenticationPrincipal OidcUser user,@RequestHeader("X-Access-Context") UUID context,@RequestBody InvitationService.Create body){
        return response(service.create(AdminCaller.from(user),context,body),201);
    }
    @PostMapping("/api/v1/admin/invitations/{id}/revoke")
    public ResponseEntity<?> revoke(@AuthenticationPrincipal OidcUser user,@RequestHeader("X-Access-Context") UUID context,@PathVariable UUID id,@RequestBody InvitationService.Change body){
        return response(service.revoke(AdminCaller.from(user),context,id,body),200);
    }
    @PostMapping("/api/v1/admin/invitations/{id}/retry")
    public ResponseEntity<?> retry(@AuthenticationPrincipal OidcUser user,@RequestHeader("X-Access-Context") UUID context,@PathVariable UUID id,@RequestBody InvitationService.Change body){
        return response(service.retry(AdminCaller.from(user),context,id,body),200);
    }
    @PostMapping("/api/v1/invitations/preview")
    public ResponseEntity<?> preview(@AuthenticationPrincipal OidcUser user,@RequestBody TokenRequest body){
        return response(service.preview(InvitationCaller.from(user),body.token()),200);
    }
    @PostMapping("/api/v1/invitations/accept")
    public ResponseEntity<?> accept(@AuthenticationPrincipal OidcUser user,@RequestBody TokenRequest body){
        return response(service.accept(InvitationCaller.from(user),body.token()),200);
    }
    private ResponseEntity<?> response(Object body,int status){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);}
}
