package tech.sledger.model.tx;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import tech.sledger.model.account.Account;
import java.math.BigDecimal;
import java.time.Instant;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = NAME, include = PROPERTY)
@JsonSubTypes({
    @JsonSubTypes.Type(value = CashTransaction.class, name = "cash"),
    @JsonSubTypes.Type(value = CreditTransaction.class, name = "credit"),
    @JsonSubTypes.Type(value = ForeignCashTransaction.class, name = "fx")
})
public class Transaction {
    @Id
    private long id;
    @DBRef
    @JsonIdentityReference(alwaysAsId = true)
    private Account account;
    private Instant date;
    private BigDecimal amount;
    private BigDecimal balance;
}
