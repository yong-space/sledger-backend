package tech.sledger.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Insight {
    private int year;
    private int month;
    private String category;
    private int transactions;
    private BigDecimal total;
}
