package com.aiexpenseledger.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Turns net balances into a short list of "who pays whom" transfers by repeatedly matching the largest
 * debtor with the largest creditor. Produces at most n-1 transfers for n users.
 */
public final class SettlementCalculator {

    private SettlementCalculator() {
    }

    public static List<Settlement> settle(Map<Long, BigDecimal> netBalances) {
        List<Position> creditors = new ArrayList<>();
        List<Position> debtors = new ArrayList<>();
        netBalances.forEach((userId, net) -> {
            int sign = net.signum();
            if (sign > 0) {
                creditors.add(new Position(userId, net));
            } else if (sign < 0) {
                debtors.add(new Position(userId, net.negate()));
            }
        });

        Comparator<Position> largestFirst = Comparator.comparing((Position p) -> p.remaining).reversed()
                .thenComparing(p -> p.userId);
        creditors.sort(largestFirst);
        debtors.sort(largestFirst);

        List<Settlement> settlements = new ArrayList<>();
        int c = 0;
        int d = 0;
        while (c < creditors.size() && d < debtors.size()) {
            Position creditor = creditors.get(c);
            Position debtor = debtors.get(d);
            BigDecimal amount = creditor.remaining.min(debtor.remaining);

            settlements.add(new Settlement(debtor.userId, creditor.userId, amount));
            creditor.remaining = creditor.remaining.subtract(amount);
            debtor.remaining = debtor.remaining.subtract(amount);

            if (creditor.remaining.signum() == 0) c++;
            if (debtor.remaining.signum() == 0) d++;
        }
        return settlements;
    }

    private static final class Position {
        private final Long userId;
        private BigDecimal remaining;

        private Position(Long userId, BigDecimal remaining) {
            this.userId = userId;
            this.remaining = remaining;
        }
    }
}
