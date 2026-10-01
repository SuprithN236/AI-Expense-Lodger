package com.aiexpenseledger.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A natural-language question about one group's spending, e.g. "How much did we spend on groceries last week?". */
public record SpendingQueryRequest(
        @NotBlank @Size(max = 500) String queryText,
        @NotNull Long groupId) {
}
