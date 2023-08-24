package tech.sledger.model.tx;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Template {
    @Id
    private long id;
    private long ownerId;
    private String reference;
    private String remarks;
    private String category;
    private String subCategory;
}
