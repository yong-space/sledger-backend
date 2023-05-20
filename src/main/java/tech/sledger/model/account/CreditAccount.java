package tech.sledger.model.account;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@Document("account")
public class CreditAccount extends Account {
    private int billingCycle;
    private long paymentAccountId;
    private String paymentRemarks;
}
