package tech.sledger.model.tx;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class ForeignCashTransaction extends CashTransaction {
    private String currency;
    private BigDecimal originalAmount;
}
