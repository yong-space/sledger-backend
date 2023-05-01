package tech.sledger.model.tx;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import tech.sledger.model.user.User;

@Data
@Builder
public class Template {
    @Id
    private long id;
    @DBRef
    @JsonIdentityReference(alwaysAsId = true)
    private User owner;
    private String reference;
    private String remarks;
    private String category;
}
