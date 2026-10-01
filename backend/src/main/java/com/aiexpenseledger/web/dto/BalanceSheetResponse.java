package com.aiexpenseledger.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * @param balances    per-member net position: positive = is owed money, negative = owes money
 * @param settlements the fewest transfers that bring every balance to zero
 */
public record BalanceSheetResponse(
        Long groupId,
        BigDecimal totalSpent,
        List<MemberBalance> balances,
        List<SettlementResponse> settlements) {

    public record MemberBalance(Long userId, String email, BigDecimal netBalance) {
    }

    public record SettlementResponse(Long fromUserId, String fromEmail, Long toUserId, String toEmail, BigDecimal amount) {
    }
}
