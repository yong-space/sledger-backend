package tech.sledger.service;

import com.github.jknack.handlebars.Handlebars;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    @Value("${sledger.uri}")
    private String baseUri;
    @Value("${sledger.from-email}")
    private String fromEmail;
    private final JavaMailSender mailSender;
    private final Handlebars handlebars = new Handlebars();

    @Async
    public CompletableFuture<Boolean> sendActivation(String toEmail, String displayName, String hash) {
        Map<String, String> data = Map.of(
            "name", displayName,
            "requestUrl", baseUri + "/api/public/activate/" + hash
        );
        String content = compileTemplate("activation", data);
        sendEmail(toEmail, displayName, "Sledger Activation", content);
        return CompletableFuture.completedFuture(true);
    }

    public String compileTemplate(String template, Map<String, String> data) {
        try {
            return handlebars.compile("email/" + template).apply(data);
        } catch (IOException e) {
            log.error("Unable to load template: {}", template);
            return null;
        }
    }

    void sendEmail(String toEmail, String toDisplayName, String subject, String content) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.toString());
            helper.setFrom(fromEmail);
            helper.setTo(toDisplayName + " <" + toEmail + ">");
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Unable to send email", e);
        }
    }
}
