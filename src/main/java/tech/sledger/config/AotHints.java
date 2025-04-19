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
import org.springframework.lang.NonNull;
import tech.sledger.model.dto.CategoryInsight;
import tech.sledger.model.dto.ChartResponse;
import tech.sledger.model.portfolio.EmailSnapshot;
import tech.sledger.model.portfolio.PortfolioPosition;
import tech.sledger.model.portfolio.PortfolioSnapshot;
import tech.sledger.model.portfolio.PortfolioSummary;
import tech.sledger.model.tx.CashTransaction;
import tech.sledger.model.tx.CpfTransaction;
import tech.sledger.model.tx.CreditTransaction;
import tech.sledger.model.tx.ForeignCashTransaction;
import tech.sledger.model.tx.ForeignCreditTransaction;
import tech.sledger.model.tx.Transaction;
import tech.sledger.service.importer.GrabImporter;
import tech.sledger.service.importer.OcbcImporter;
import tech.sledger.service.importer.UobImporter;
import java.util.List;

public class AotHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
        List.of(
            Transaction.class,
            CashTransaction.class,
            CreditTransaction.class,
            CpfTransaction.class,
            ForeignCashTransaction.class,
            ForeignCreditTransaction.class,
            CategoryInsight.class,
            ChartResponse.class,
            OcbcImporter.class,
            UobImporter.class,
            GrabImporter.class,
            HSSFWorkbook.class,
            PortfolioSummary.class,
            PortfolioSnapshot.class,
            DefaultFlowMessageFactory.class,
            ParameterizedMessageFactory.class,
            DefaultHelperRegistry.class,
            PortfolioPosition.class,
            EmailSnapshot.class,
            CreateEmailOptions.class,
            CreateEmailResponse.class
        ).forEach(c -> hints.reflection().registerType(c, MemberCategory.values()));

        hints.resources().registerPattern("email/*");
    }
}
