package com.aiexpenseledger.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReceiptTextRequest(@NotBlank @Size(max = 10_000) String text) {
}
