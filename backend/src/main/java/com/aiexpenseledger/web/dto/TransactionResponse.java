package com.aiexpenseledger.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TransactionResponse(
        Long id,
        Long groupId,
        String description,
        BigDecimal totalAmount,
        Long payerId,
        String payerEmail,
        LocalDate date,
        Long createdBy,
        Instant createdAt,
        Long reversesTransactionId,
        Long reversedByTransactionId,
        List<SplitResponse> splits) {

    public record SplitResponse(Long userId, String email, BigDecimal owedAmount) {
    }
}
