package tech.sledger.model.account;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import tech.sledger.model.user.User;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Account {
    @Id
    @EqualsAndHashCode.Include
    private long id;
    @DBRef
    @JsonIdentityReference(alwaysAsId = true)
    private User owner;
    @DBRef
    private AccountIssuer issuer;
    private AccountType type;
    private String name;
    @Builder.Default
    private boolean visible = true;
    private int sortOrder;
}
