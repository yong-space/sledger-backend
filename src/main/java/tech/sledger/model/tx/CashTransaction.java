package tech.sledger.model.tx;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
public class CashTransaction extends Transaction {
    @Indexed
    private String category;
    @Indexed
    private String subCategory;
    @Indexed
    private String remarks;
}
