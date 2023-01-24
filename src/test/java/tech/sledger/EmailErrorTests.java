package tech.sledger;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import tech.sledger.service.EmailService;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
public class EmailErrorTests {
    static {
        System.setProperty("MONGO_URI", "mongodb://localhost/sledger");
    }

    @Autowired
    private EmailService emailService;
    @MockBean
    private JavaMailSender mailSender;

    @Test
    public void sendFail() {
        String content = emailService.compileTemplate("abc", Map.of("a", "b"));
        assertNull(content);
    }

    @Test
    public void sendError() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        Mockito.doThrow(RuntimeException.class).doNothing().when(mailSender).send(any(MimeMessage.class));
        assertThrows(RuntimeException.class, () ->
            emailService.sendActivation("user@company.com", "Bob Doe", "hash").join());
    }
}
