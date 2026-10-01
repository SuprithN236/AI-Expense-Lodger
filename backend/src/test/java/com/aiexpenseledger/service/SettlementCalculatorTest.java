package com.aiexpenseledger.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementCalculatorTest {

    @Test
    void settlesEveryBalanceToZero() {
        Map<Long, BigDecimal> balances = new TreeMap<>(Map.of(
                1L, new BigDecimal("60.00"),
                2L, new BigDecimal("-20.00"),
                3L, new BigDecimal("-40.00")));

        List<Settlement> settlements = SettlementCalculator.settle(balances);

        assertEquals(List.of(
                new Settlement(3L, 1L, new BigDecimal("40.00")),
                new Settlement(2L, 1L, new BigDecimal("20.00"))), settlements);
    }

    @Test
    void splitsOneDebtAcrossSeveralCreditors() {
        Map<Long, BigDecimal> balances = new TreeMap<>(Map.of(
                1L, new BigDecimal("25.50"),
                2L, new BigDecimal("10.25"),
                3L, new BigDecimal("-35.75")));

        List<Settlement> settlements = SettlementCalculator.settle(balances);

        assertEquals(List.of(
                new Settlement(3L, 1L, new BigDecimal("25.50")),
                new Settlement(3L, 2L, new BigDecimal("10.25"))), settlements);
    }

    @Test
    void settledGroupNeedsNoTransfers() {
        assertTrue(SettlementCalculator.settle(Map.of(1L, new BigDecimal("0.00"), 2L, new BigDecimal("0.00"))).isEmpty());
    }
}
