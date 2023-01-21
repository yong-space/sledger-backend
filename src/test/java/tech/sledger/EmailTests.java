package tech.sledger;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tech.sledger.service.EmailService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class EmailTests {
    static {
        System.setProperty("MONGO_URI", "mongodb://localhost/sledger");
        System.setProperty("EMAIL_USERNAME", "user");
        System.setProperty("EMAIL_PASSWORD", "pass");
    }

    @Autowired
    private EmailService emailService;

    @RegisterExtension
    public static final GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP_IMAP)
        .withConfiguration(GreenMailConfiguration.aConfig().withUser("user@company.com", "user", "pass"));

    @Test
    public void sendActivation() throws MessagingException {
        emailService.sendActivation("user@company.com", "Bob Doe", "hash").join();
        MimeMessage receivedMessage = greenMail.getReceivedMessages()[0];
        assertEquals("Sledger Activation", receivedMessage.getSubject());
        assertEquals("Bob Doe <user@company.com>", receivedMessage.getAllRecipients()[0].toString());
        assertTrue(GreenMailUtil.getBody(receivedMessage).contains("Hello Bob Doe,"));
        assertTrue(GreenMailUtil.getBody(receivedMessage).contains("/activate/hash"));
    }
}
