package tech.sledger.service;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
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
        ClassPathTemplateLoader loader = new ClassPathTemplateLoader("/email", ".hbs");
        handlebars = new Handlebars(loader);

        handlebars.registerHelper("formatCurrency", (Helper<BigDecimal>) (value, _) -> {
            if (value == null) return "";
            NumberFormat format = NumberFormat.getNumberInstance();
            format.setMaximumFractionDigits(2);
            format.setMinimumFractionDigits(2);
            return format.format(value);
        });

        handlebars.registerHelper("formatPercentage", (Helper<BigDecimal>) (value, _) -> {
            if (value == null) return "";
            return String.format("%.2f%%", value);
        });

        handlebars.registerHelper("signClass", (Helper<BigDecimal>) (value, _) -> {
            if (value == null) return "";
            return value.compareTo(BigDecimal.ZERO) >= 0 ? "positive" : "negative";
        });
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
        Template template = null;
        try {
            template = handlebars.compile(templateName);
            return template.apply(data);
        } catch (IOException e) {
            log.error("Unable to load template: {}", template);
            return null;
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
