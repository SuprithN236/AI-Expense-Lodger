package com.aiexpenseledger.web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Exactly one of {@code splits} (explicit user id → owed amount) or {@code participantIds}
 * (split equally, to the cent) must be provided.
 *
 * @param idempotencyKey generated once per form submission by the client; replays are rejected with 409
 * @param payerId        defaults to the authenticated user
 * @param date           defaults to today
 */
public record LogExpenseRequest(
        @NotNull UUID idempotencyKey,
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive BigDecimal totalAmount,
        Long payerId,
        LocalDate date,
        Map<Long, BigDecimal> splits,
        List<Long> participantIds) {

    @AssertTrue(message = "Provide either splits or participantIds, but not both")
    public boolean isSplitModeValid() {
        boolean hasSplits = splits != null && !splits.isEmpty();
        boolean hasParticipants = participantIds != null && !participantIds.isEmpty();
        return hasSplits ^ hasParticipants;
    }
}
