package tech.sledger.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CategoryInsight {
    private String category;
    private int transactions;
    private BigDecimal average;
}
