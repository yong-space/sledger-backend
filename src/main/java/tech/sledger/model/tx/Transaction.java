package tech.sledger.model.tx;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CashTransaction.class, name = "cash"),
    @JsonSubTypes.Type(value = CreditTransaction.class, name = "credit"),
    @JsonSubTypes.Type(value = ForeignCashTransaction.class, name = "fx-cash"),
    @JsonSubTypes.Type(value = ForeignCreditTransaction.class, name = "fx-credit"),
    @JsonSubTypes.Type(value = CpfTransaction.class, name = "retirement"),
})
public class Transaction {
    @Id
    private long id;
    private long accountId;
    private Instant date;
    private BigDecimal amount;
    private BigDecimal balance;
}
