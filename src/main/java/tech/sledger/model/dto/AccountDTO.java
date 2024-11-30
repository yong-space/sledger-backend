package tech.sledger.model.dto;

import lombok.Data;
import tech.sledger.model.account.AccountType;
import java.math.BigDecimal;

@Data
public class AccountDTO {
    private long id;
    private long issuerId;
    private AccountType type;
    private int sortOrder;
    private String name;
    private boolean visible;
    private BigDecimal balance;
    private long transactions;
    private boolean multiCurrency;
    private Integer billingCycle;
    private Long paymentAccountId;
    private String paymentRemarks;
    private BigDecimal ordinaryRatio;
    private BigDecimal specialRatio;
    private BigDecimal medisaveRatio;
    private BigDecimal ordinaryBalance;
    private BigDecimal specialBalance;
    private BigDecimal medisaveBalance;
}
