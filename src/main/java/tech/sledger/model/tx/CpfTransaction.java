package tech.sledger.model.tx;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class CpfTransaction extends Transaction {
    private Instant forMonth;
    private String code;
    private String company;
    private BigDecimal ordinaryAmount;
    private BigDecimal specialAmount;
    private BigDecimal medisaveAmount;
}
