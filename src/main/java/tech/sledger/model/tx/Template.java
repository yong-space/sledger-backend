package tech.sledger.model.tx;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotNull(message = "Reference cannot be null")
    @Size(min = 3, max = 30, message = "Reference should be between 3 and 30 characters")
    private String reference;
    @NotNull(message = "Remarks cannot be null")
    @Size(min = 3, max = 50, message = "Remarks should be between 3 and 50 characters")
    private String remarks;
    @NotNull(message = "Category cannot be null")
    @Size(min = 3, max = 30, message = "Category should be between 3 and 30 characters")
    private String category;
    @NotNull(message = "Sub-category cannot be null")
    @Size(min = 3, max = 30, message = "Sub-category should be between 3 and 30 characters")
    private String subCategory;
}
