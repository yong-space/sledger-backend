package tech.sledger.model.tx;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
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
