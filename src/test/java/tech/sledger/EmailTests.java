package tech.sledger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.aot.DisabledInAotMode;
import tech.sledger.service.EmailService;
import tech.sledger.service.ResendService;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DisabledInAotMode
@SpringBootTest
public class EmailTests {
    static {
        System.setProperty("MONGO_URI", "mongodb://localhost/sledger");
    }

    @Autowired
    private EmailService emailService;

    @MockBean
    private ResendService resendService;

    @Test
    public void sendFail() {
        String content = emailService.compileTemplate("abc", Map.of("a", "b"));
        assertNull(content);
    }

    @Captor
    ArgumentCaptor<CreateEmailOptions> emailCaptor;

    @Test
    public void sendActivation() {
        when(resendService.send(any(CreateEmailOptions.class)))
            .thenReturn(new CreateEmailResponse("abc"));

        emailService.sendActivation("user@company.com", "Bob Doe", "hash").join();
        verify(resendService).send(emailCaptor.capture());
        CreateEmailOptions email = emailCaptor.getValue();

        assertEquals("Sledger Activation", email.getSubject());
        assertEquals("Bob Doe <user@company.com>", email.getTo().getFirst());
        assertTrue(email.getHtml().contains("Hello Bob Doe,"));
        assertTrue(email.getHtml().contains("/activate/hash"));
    }
}
