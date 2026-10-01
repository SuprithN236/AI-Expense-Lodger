package com.aiexpenseledger.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Structured fields extracted from a receipt by the LLM. This is only a suggestion used to pre-fill the
 * expense form; nothing is written to the ledger until a user submits that form.
 *
 * @param date ISO-8601 date (yyyy-MM-dd), or null when the receipt shows none
 */
public record ReceiptData(String merchant, BigDecimal totalAmount, List<String> items, String date) {
}
