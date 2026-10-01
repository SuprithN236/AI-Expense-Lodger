package com.aiexpenseledger.service;

import com.aiexpenseledger.exception.LedgerValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void equalSplitDistributesRemainderCentsSoPartsSumToTotal() {
        Map<Long, BigDecimal> shares = Money.equalSplit(new BigDecimal("10.00"), List.of(3L, 1L, 2L));

        assertEquals(new BigDecimal("3.34"), shares.get(1L));
        assertEquals(new BigDecimal("3.33"), shares.get(2L));
        assertEquals(new BigDecimal("3.33"), shares.get(3L));
        assertEquals(new BigDecimal("10.00"), shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    void equalSplitIgnoresDuplicateParticipants() {
        Map<Long, BigDecimal> shares = Money.equalSplit(new BigDecimal("9"), List.of(1L, 1L, 2L));

        assertEquals(2, shares.size());
        assertEquals(new BigDecimal("4.50"), shares.get(1L));
        assertEquals(new BigDecimal("4.50"), shares.get(2L));
    }

    @Test
    void normalizeRejectsSubCentPrecision() {
        assertThrows(LedgerValidationException.class, () -> Money.normalize(new BigDecimal("10.005"), "amount"));
    }

    @Test
    void normalizeRescalesWholeNumbers() {
        assertEquals(new BigDecimal("42.00"), Money.normalize(new BigDecimal("42"), "amount"));
    }
}
