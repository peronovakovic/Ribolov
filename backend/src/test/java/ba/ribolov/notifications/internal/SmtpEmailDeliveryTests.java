package ba.ribolov.notifications.internal;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SmtpEmailDeliveryTests {
 @Test void sendsUtf8MessageThroughRealLocalSmtpTransport() throws Exception {
  try(var server=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var executor=Executors.newSingleThreadExecutor()){
   var received=executor.submit(()->receive(server));
   new SmtpEmailDelivery("127.0.0.1",server.getLocalPort(),"noreply@example.test","","",false)
     .send("member@example.test","Poziv za udruženje","Prihvati poziv: http://localhost/#invite=test");
   var message=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(received.get(10,TimeUnit.SECONDS).getBytes(StandardCharsets.US_ASCII)));
   assertThat(message.getSubject()).isEqualTo("Poziv za udruženje");
   assertThat(message.getContent().toString()).contains("#invite=test");
   assertThat(message.getAllRecipients()[0].toString()).isEqualTo("member@example.test");
  }
 }
 @Test void requiredTlsFailsClosedWithoutExposingRecipient() throws Exception {
  try(var server=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var executor=Executors.newSingleThreadExecutor()){
   var received=executor.submit(()->receive(server));
   assertThatThrownBy(()->new SmtpEmailDelivery("127.0.0.1",server.getLocalPort(),"noreply@example.test","","",true)
     .send("private@example.test","Secret","secret-token")).isInstanceOf(IllegalStateException.class).hasMessage("Email delivery failed").hasNoCause();
   assertThat(received.get(10,TimeUnit.SECONDS)).isEmpty();
  }
 }
 private String receive(ServerSocket server)throws Exception {
  server.setSoTimeout(10000);
  try(var socket=server.accept()){
   socket.setSoTimeout(10000);
   var in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
   var out=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.US_ASCII),true);
   out.print("220 localhost test\r\n");out.flush();var data=new StringBuilder();boolean body=false;
   for(String line;(line=in.readLine())!=null;){
    if(body){if(line.equals(".")){body=false;out.print("250 accepted\r\n");}else data.append(line).append("\r\n");}
    else if(line.startsWith("EHLO")||line.startsWith("HELO"))out.print("250 localhost\r\n");
    else if(line.startsWith("MAIL")||line.startsWith("RCPT"))out.print("250 OK\r\n");
    else if(line.equals("DATA")){body=true;out.print("354 send\r\n");}
    else if(line.equals("QUIT")){out.print("221 bye\r\n");out.flush();break;}
    else out.print("250 OK\r\n");
    out.flush();
   }
   return data.toString();
  }
 }
}
