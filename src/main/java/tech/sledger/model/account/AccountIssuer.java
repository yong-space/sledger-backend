package tech.sledger.model.account;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

@Data
@NoArgsConstructor
public class AccountIssuer {
    @Id
    private long id;
    @Indexed(unique = true)
    private String name;
}
