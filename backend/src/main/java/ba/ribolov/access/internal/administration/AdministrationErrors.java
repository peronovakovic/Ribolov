package ba.ribolov.access.internal.administration;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = {AdministrationController.class, ba.ribolov.access.internal.invitations.InvitationController.class})
class AdministrationErrors {
    @ExceptionHandler(ReauthenticationRequired.class)
    ResponseEntity<ProblemDetail> reauthenticate(ReauthenticationRequired error) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, error.getMessage());
        problem.setProperty("reauthenticate", true);
        return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).body(problem);
    }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> denied(AccessDeniedException error) { return problem(HttpStatus.FORBIDDEN, error.getMessage()); }
    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ProblemDetail> missing(EntityNotFoundException error) { return problem(HttpStatus.NOT_FOUND, "Zapis nije dostupan."); }
    @ExceptionHandler({OptimisticLockException.class, org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ResponseEntity<ProblemDetail> stale(Exception error) { return problem(HttpStatus.CONFLICT, "Podaci su promijenjeni. OsvjeĹľite pregled i ponovite izmjenu."); }
    @ExceptionHandler({DataIntegrityViolationException.class, org.hibernate.exception.ConstraintViolationException.class})
    ResponseEntity<ProblemDetail> conflict(Exception error) { return problem(HttpStatus.CONFLICT, "Zapis veÄ‡ postoji, periodi se preklapaju ili povezana evidencija nije ispravna."); }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> state(IllegalStateException error) { return problem(HttpStatus.CONFLICT, error.getMessage()); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> invalid(IllegalArgumentException error) { return problem(HttpStatus.BAD_REQUEST, error.getMessage()); }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
