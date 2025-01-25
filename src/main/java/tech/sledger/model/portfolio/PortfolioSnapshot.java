package tech.sledger.model.portfolio;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import tech.sledger.model.user.User;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PortfolioSnapshot {
    @Id
    @EqualsAndHashCode.Include
    private long id;
    @DBRef
    @JsonIdentityReference(alwaysAsId = true)
    private User owner;
    private BigDecimal holdings;
    private BigDecimal cash;
    private BigDecimal fx;
    private Instant time;
    private String broker;
    private String brokerColour;
}
