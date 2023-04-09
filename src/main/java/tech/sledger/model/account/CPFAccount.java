package tech.sledger.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@Document("account")
public class CPFAccount extends Account {
    private BigDecimal ordinaryRatio;
    private BigDecimal specialRatio;
    private BigDecimal medisaveRatio;
}
