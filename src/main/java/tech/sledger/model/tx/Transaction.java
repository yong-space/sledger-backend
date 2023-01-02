package tech.sledger.model.tx;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import tech.sledger.model.account.Account;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
public abstract class Transaction {
    @Id
    private long id;
    @DBRef
    private Account account;
    private Instant date;
    private BigDecimal amount;
}
