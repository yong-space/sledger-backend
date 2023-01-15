package tech.sledger.model.user;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.Instant;

@Data
@Builder
public class Activation {
    @Id
    private String code;
    @DBRef
    private User user;
    private Instant date;
}
