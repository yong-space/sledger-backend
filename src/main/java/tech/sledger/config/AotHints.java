package tech.sledger.config;

import com.github.jknack.handlebars.helper.DefaultHelperRegistry;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import tech.sledger.model.dto.AccountDTO;
import tech.sledger.model.dto.CategoryInsight;
import tech.sledger.model.dto.CategorySuggestion;
import tech.sledger.model.dto.Insight;
import tech.sledger.model.dto.MonthlyBalance;
import tech.sledger.model.portfolio.EmailSnapshot;
import tech.sledger.model.portfolio.PortfolioPosition;
import tech.sledger.model.portfolio.PortfolioSummary;
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
            AccountDTO.class,
            CategoryInsight.class,
            CategorySuggestion.class,
            Insight.class,
            MonthlyBalance.class,
            EmailSnapshot.class,
            PortfolioPosition.class,
            PortfolioSummary.class
        ).forEach(c -> hints.reflection().registerType(c, MemberCategory.values()));

        // Caffeine picks its bounded-cache and node implementations reflectively via a name that
        // encodes the enabled features. The weighted (txAll) and size-based (authorise) variants
        // aren't in the default reachability metadata, so register the exact classes our CacheConfig
        // builders select (discovered by instantiating the same builders on the JVM).
        // Caffeine instantiates these via a reflectively-read static FACTORY field plus a constructor,
        // so register full reflection (fields + constructors), not just constructors.
        List.of(
            "com.github.benmanes.caffeine.cache.SSMWA", // txAll cache: strong keys/values, max-weight, access-expiry
            "com.github.benmanes.caffeine.cache.PSAMW", // txAll node
            "com.github.benmanes.caffeine.cache.SSMSA", // authorise cache: strong keys/values, max-size, access-expiry
            "com.github.benmanes.caffeine.cache.PSAMS"  // authorise node
        ).forEach(c -> hints.reflection().registerType(TypeReference.of(c), MemberCategory.values()));

        hints.resources().registerPattern("email/*");
    }
}
