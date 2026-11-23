package ba.ribolov.access.api;

/** Authentication information only; it never grants organization or business permissions. */
public record SessionView(String subject, String displayName) {}
