package tech.sledger.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResendService {
    @Value("${sledger.resend-key}")
    private String resendKey;

    public CreateEmailResponse send(CreateEmailOptions request) {
        try {
            Resend resend = new Resend(resendKey);
            return resend.emails().send(request);
        } catch (ResendException e) {
            log.error("Error sending email", e);
            return null;
        }
    }
}
