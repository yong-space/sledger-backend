package tech.sledger.model.account;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewAccount {
    private String name;
    private AccountType type;
    private long issuerId;
    private int billingCycle;
    private int billingMonthOffset;
    private boolean multiCurrency;
    private BigDecimal ordinaryRatio;
    private BigDecimal specialRatio;
    private BigDecimal medisaveRatio;
}
