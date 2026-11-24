package ba.ribolov.notifications.api;
/** Transport only; the calling module owns its durable delivery queue. Never log message contents. */
public interface EmailDelivery {
    void send(String recipient, String subject, String text);
}
