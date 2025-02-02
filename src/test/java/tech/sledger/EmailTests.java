package tech.sledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.sledger.service.EmailService;
import tech.sledger.service.ResendService;
import java.util.Map;

@DisabledInAotMode
public class EmailTests extends BaseTest {
    @Autowired
    private EmailService emailService;

    @MockitoBean
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
