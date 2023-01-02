package tech.sledger.model.account;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@NoArgsConstructor
public class AccountIssuer {
    @Id
    private long id;
    private String name;
}
