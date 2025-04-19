package tech.sledger.service;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
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
    private Handlebars handlebars;

    @PostConstruct
    public void init() {
        Helper<BigDecimal> formatCurrency = (value, _) -> {
            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMaximumFractionDigits(0);
            format.setMinimumFractionDigits(0);
            return format.format(value);
        };
        Helper<BigDecimal> formatPercentage = (value, _) -> String.format("%.1f%%", value);
        Helper<BigDecimal> signClass = (value, _) -> {
            int i = value.compareTo(BigDecimal.ZERO);
            return i == 0 ? "" : i > 0 ? "positive" : "negative";
        };

        ClassPathTemplateLoader loader = new ClassPathTemplateLoader("/email", ".hbs");
        handlebars = new Handlebars(loader);
        handlebars.registerHelper("formatCurrency", formatCurrency);
        handlebars.registerHelper("formatPercentage", formatPercentage);
        handlebars.registerHelper("signClass", signClass);
    }

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

    public String compileTemplate(String templateName, Object data) {
        try {
            return handlebars.compile(templateName).apply(data);
        } catch (IOException e) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unable to compile template: " + templateName, e);
        }
    }

    public void sendEmail(String toEmail, String toDisplayName, String subject, String content) {
        CreateEmailOptions options = CreateEmailOptions.builder()
            .from(fromEmail)
            .to(toDisplayName + " <" + toEmail + ">")
            .subject(subject)
            .html(content)
            .build();
        CreateEmailResponse data = resendService.send(options);
        log.info("Email sent: {}", data.getId());
    }
}
