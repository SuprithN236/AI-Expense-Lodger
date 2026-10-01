package com.aiexpenseledger.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReverseTransactionRequest(@NotNull UUID idempotencyKey) {
}
