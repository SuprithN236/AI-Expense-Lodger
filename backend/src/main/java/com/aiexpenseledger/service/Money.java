package com.aiexpenseledger.service;

import com.aiexpenseledger.exception.LedgerValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** BigDecimal helpers for two-decimal currency amounts. No floating-point arithmetic is ever used. */
public final class Money {

    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private Money() {
    }

    /** Rescales to exactly two decimals, rejecting values that would need rounding (e.g. 10.005). */
    public static BigDecimal normalize(BigDecimal amount, String field) {
        if (amount == null) {
            throw new LedgerValidationException(field + " is required");
        }
        try {
            BigDecimal scaled = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
            if (scaled.abs().compareTo(MAX_AMOUNT) > 0) {
                throw new LedgerValidationException(field + " exceeds the maximum supported amount");
            }
            return scaled;
        } catch (ArithmeticException e) {
            throw new LedgerValidationException(field + " must have at most two decimal places");
        }
    }

    /**
     * Splits {@code total} evenly across {@code participantIds}, distributing leftover cents one at a time
     * to the first participants so the parts always sum exactly to the total.
     * Example: 10.00 across 3 users → 3.34, 3.33, 3.33.
     */
    public static Map<Long, BigDecimal> equalSplit(BigDecimal total, List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new LedgerValidationException("At least one participant is required to split an expense");
        }
        List<Long> ordered = new ArrayList<>(participantIds.stream().distinct().sorted().toList());
        long totalCents = normalize(total, "totalAmount").movePointRight(SCALE).longValueExact();
        long baseCents = totalCents / ordered.size();
        long remainder = totalCents % ordered.size();

        Map<Long, BigDecimal> shares = new LinkedHashMap<>();
        for (int i = 0; i < ordered.size(); i++) {
            long cents = baseCents + (i < remainder ? 1 : 0);
            shares.put(ordered.get(i), BigDecimal.valueOf(cents, SCALE));
        }
        return shares;
    }
}
