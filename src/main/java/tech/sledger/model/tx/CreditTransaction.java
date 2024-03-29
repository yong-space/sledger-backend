package tech.sledger.model.tx;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class CreditTransaction extends CashTransaction {
    private Instant billingMonth;
}
