package tech.sledger.model.account;

import lombok.Data;
import tech.sledger.model.user.User;
import java.math.BigDecimal;

@Data
public class AccountDTO {
    private long id;
    private User owner;
    private AccountIssuer issuer;
    private AccountType type;
    private String name;
    private long transactions;
    private BigDecimal balance = BigDecimal.ZERO;
    private boolean visible;
}
