package tech.sledger.config;

import static org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS;
import static org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS;
import static org.springframework.aot.hint.MemberCategory.PUBLIC_FIELDS;
import org.apache.logging.log4j.message.DefaultFlowMessageFactory;
import org.apache.logging.log4j.message.ParameterizedMessageFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;
import tech.sledger.model.dto.CategoryInsight;
import tech.sledger.model.dto.ChartResponse;
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
            DefaultFlowMessageFactory.class,
            ParameterizedMessageFactory.class
        ).forEach(c -> {
            hints.reflection().registerType(c, INVOKE_DECLARED_CONSTRUCTORS);
            hints.reflection().registerType(c, INVOKE_PUBLIC_CONSTRUCTORS);
            hints.reflection().registerType(c, INVOKE_DECLARED_METHODS);
            hints.reflection().registerType(c, INVOKE_PUBLIC_METHODS);
            hints.reflection().registerType(c, DECLARED_FIELDS);
            hints.reflection().registerType(c, PUBLIC_FIELDS);
        });
    }
}
