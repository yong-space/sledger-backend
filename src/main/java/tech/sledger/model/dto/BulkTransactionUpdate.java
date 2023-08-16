package tech.sledger.model.dto;

import java.time.Instant;
import java.util.List;

public record BulkTransactionUpdate(
    List<Long> ids,
    Instant billingMonth,
    String remarks,
    String category
) {}
