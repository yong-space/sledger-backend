package tech.sledger.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryInsight {
    private String category;
    private String subCategory;
    private int transactions;
    private BigDecimal average;
}
