package ba.ribolov.access.internal.administration;

import org.springframework.security.access.AccessDeniedException;

public class ReauthenticationRequired extends AccessDeniedException {
    public ReauthenticationRequired() { super("Za ovu radnju obnovite prijavu uz drugi faktor."); }
}
