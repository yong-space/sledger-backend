package tech.sledger.model.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import tech.sledger.model.user.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    @Id
    private long id;
    @DBRef
    private User owner;
    @DBRef
    private AccountIssuer issuer;
    private AccountType type;
    private String name;
}
