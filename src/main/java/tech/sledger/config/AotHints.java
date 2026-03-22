package tech.sledger.config;

import com.github.jknack.handlebars.helper.DefaultHelperRegistry;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import java.util.List;

public class AotHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        List.of(
            HSSFWorkbook.class,
            DefaultFlowMessageFactory.class,
            ParameterizedMessageFactory.class,
            DefaultHelperRegistry.class,
            CreateEmailOptions.class,
            CreateEmailResponse.class,
            BouncyCastleProvider.class
        ).forEach(c -> hints.reflection().registerType(c, MemberCategory.values()));

        hints.resources().registerPattern("email/*");
    }
}
