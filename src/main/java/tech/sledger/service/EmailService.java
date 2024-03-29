package tech.sledger.service;

import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import java.io.File;
import java.io.IOException;
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
    private final ResendService resendService;

    @Async
    public CompletableFuture<Boolean> sendActivation(String toEmail, String displayName, String hash) {
        Map<String, String> data = Map.of(
            "name", displayName,
            "requestUrl", baseUri + "/api/activate/" + hash
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

    public void sendEmail(String toEmail, String toDisplayName, String subject, String content) {
        SendEmailRequest request = SendEmailRequest.builder()
            .from(fromEmail)
            .to(toDisplayName + " <" + toEmail + ">")
            .subject(subject)
            .html(content)
            .build();
        SendEmailResponse data = resendService.send(request);
        log.info("Email sent: {}", data.getId());
    }
}
