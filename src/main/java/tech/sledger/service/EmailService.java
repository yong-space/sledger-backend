package tech.sledger.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    @Async
    public CompletableFuture<Boolean> sendActivation(String toEmail, String displayName, String hash) throws MessagingException {
        Map<String, String> data = Map.of(
            "name", displayName,
            "requestUrl", baseUri + "/api/public/activate/" + hash
        );
        String content = compileTemplate("activation", data);
        sendEmail(toEmail, displayName, "Sledger Activation", content);
        log.info("Activation email sent to: {}", toEmail);
        return CompletableFuture.completedFuture(true);
    }

    public String compileTemplate(String template, Map<String, String> data) {
        try {
            File file = ResourceUtils.getFile("classpath:email/" + template + ".hbs");
            String content = new String(Files.readAllBytes(file.toPath()));
            for (String key : data.keySet()) {
                content = content.replaceAll("\\{\\{" + key + "}}", data.get(key));
            }
            return content;
        } catch (IOException e) {
            log.error("Unable to load template: {}", template);
            return null;
        }
    }

    public void sendEmail(String toEmail, String toDisplayName, String subject, String content) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.toString());
        helper.setFrom(fromEmail);
        helper.setTo(toDisplayName + " <" + toEmail + ">");
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(mimeMessage);
    }
}
