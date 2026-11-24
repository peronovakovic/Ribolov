package ba.ribolov.access.internal.invitations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration(proxyBeanMethods=false)
@EnableScheduling
@ConditionalOnProperty(name="ribolov.invitations.delivery-enabled",havingValue="true")
class InvitationScheduling {
    private final InvitationDelivery delivery;
    InvitationScheduling(InvitationDelivery delivery){this.delivery=delivery;}
    @Scheduled(fixedDelayString="${ribolov.invitations.poll-interval-ms:15000}")
    void deliver() {
        for(int i=0;i<5;i++) {
            try {if(!delivery.deliverNext())return;}
            catch(RuntimeException ignored) {
                org.slf4j.LoggerFactory.getLogger(getClass()).warn("Invitation delivery transaction failed; retry on next tick.");
                return;
            }
        }
    }
}
