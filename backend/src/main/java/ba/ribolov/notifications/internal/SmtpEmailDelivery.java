package ba.ribolov.notifications.internal;
import ba.ribolov.notifications.api.EmailDelivery;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class SmtpEmailDelivery implements EmailDelivery {
    private final Session session;
    private final String from, username, password;
    SmtpEmailDelivery(@Value("${ribolov.mail.host:localhost}") String host, @Value("${ribolov.mail.port:1025}") int port,
        @Value("${ribolov.mail.from:noreply@ribolov.local}") String from,
        @Value("${ribolov.mail.username:}") String username, @Value("${ribolov.mail.password:}") String password,
        @Value("${ribolov.mail.starttls:true}") boolean tls) {
        var props=new Properties();
        props.setProperty("mail.smtp.host",host); props.setProperty("mail.smtp.port",Integer.toString(port));
        props.setProperty("mail.smtp.auth",Boolean.toString(!username.isBlank()));
        props.setProperty("mail.smtp.starttls.enable",Boolean.toString(tls));
        props.setProperty("mail.smtp.starttls.required",Boolean.toString(tls));
        props.setProperty("mail.smtp.connectiontimeout","5000"); props.setProperty("mail.smtp.timeout","5000"); props.setProperty("mail.smtp.writetimeout","5000");
        session=Session.getInstance(props); this.from=from;this.username=username;this.password=password;
    }
    public void send(String recipient,String subject,String text) {
        try {
            var message=new MimeMessage(session);
            message.setFrom(new InternetAddress(from,true));
            message.setRecipient(Message.RecipientType.TO,new InternetAddress(recipient,true));
            message.setSubject(subject,"UTF-8"); message.setText(text,"UTF-8");
            if(username.isBlank()) Transport.send(message); else Transport.send(message,username,password);
        } catch(MessagingException e) {
            // SMTP exceptions can contain recipient addresses and message fragments.
            throw new IllegalStateException("Email delivery failed");
        }
    }
}
