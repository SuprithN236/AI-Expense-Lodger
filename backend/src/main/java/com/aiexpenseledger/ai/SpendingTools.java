package com.aiexpenseledger.ai;

import com.aiexpenseledger.domain.LedgerTransaction;
import com.aiexpenseledger.service.LedgerService;
import com.aiexpenseledger.service.LedgerService.SpendingSummary;
import com.aiexpenseledger.service.SettlementCalculator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * The complete set of capabilities the LLM is given. Every method is read-only, takes typed parameters, and
 * delegates to {@link LedgerService}; the model never sees SQL or a write path.
 *
 * <p>An instance is created per request and bound to the one group the authenticated user was already
 * verified to belong to. Any attempt by the model to read a different group is refused.
 */
public class SpendingTools {

    private static final int MAX_RANGE_DAYS = 3660;
    private static final int MAX_TRANSACTIONS_RETURNED = 100;
    private static final int MAX_KEYWORDS = 10;

    private final LedgerService ledgerService;
    private final long authorizedGroupId;
    private final Map<Long, String> memberEmails;

    public SpendingTools(LedgerService ledgerService, long authorizedGroupId, Map<Long, String> memberEmails) {
        this.ledgerService = ledgerService;
        this.authorizedGroupId = authorizedGroupId;
        this.memberEmails = Map.copyOf(memberEmails);
    }

    public record GroupTotal(long groupId, BigDecimal totalSpent) {
    }

    public record LedgerRow(long transactionId, String date, String description, BigDecimal amount, String paidBy) {
    }

    public record SpendingReport(long groupId, String startDate, String endDate, List<String> keywordsApplied,
                                 BigDecimal totalSpent, int matchingTransactionCount, List<LedgerRow> transactions) {
    }

    public record MemberBalance(String member, BigDecimal netBalance) {
    }

    public record Transfer(String from, String to, BigDecimal amount) {
    }

    public record BalanceReport(long groupId, List<MemberBalance> balances, List<Transfer> suggestedSettlements) {
    }

    @Tool(name = "fetchGroupTotalSpent",
            description = "Returns the all-time total amount spent by the group, summed from every ledger entry "
                    + "(reversed entries are already netted out).")
    public GroupTotal fetchGroupTotalSpent(
            @ToolParam(description = "Id of the group being asked about") Long groupId) {
        requireAuthorizedGroup(groupId);
        return new GroupTotal(authorizedGroupId, ledgerService.fetchGroupTotalSpent(authorizedGroupId));
    }

    @Tool(name = "fetchGroupSpendingBetween",
            description = "Returns the total spent by the group between two dates (inclusive) plus the matching "
                    + "ledger entries. Optionally restrict to entries whose description contains ANY of the given "
                    + "keywords (case-insensitive substring match). Use this for any question about a time period "
                    + "or a spending category such as groceries, rent or travel.")
    public SpendingReport fetchGroupSpendingBetween(
            @ToolParam(description = "Id of the group being asked about") Long groupId,
            @ToolParam(description = "Inclusive start date in ISO format yyyy-MM-dd") String startDate,
            @ToolParam(description = "Inclusive end date in ISO format yyyy-MM-dd") String endDate,
            @ToolParam(required = false, description = "Optional lowercase keywords to match against descriptions, "
                    + "e.g. [\"grocer\", \"supermarket\"]. Omit to include every entry in the date range.")
            List<String> descriptionKeywords) {
        requireAuthorizedGroup(groupId);
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        if (ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("Date range is too large; use at most " + MAX_RANGE_DAYS + " days");
        }

        List<String> keywords = descriptionKeywords == null ? List.of()
                : descriptionKeywords.stream().limit(MAX_KEYWORDS).toList();

        SpendingSummary summary = ledgerService.summarizeSpending(
                authorizedGroupId, start, end, keywords, MAX_TRANSACTIONS_RETURNED);
        return new SpendingReport(summary.groupId(), summary.startDate().toString(), summary.endDate().toString(),
                summary.keywords(), summary.totalSpent(), summary.transactionCount(),
                summary.transactions().stream().map(this::toRow).toList());
    }

    @Tool(name = "fetchGroupBalances",
            description = "Returns each member's current net balance (positive = is owed money, negative = owes "
                    + "money) and a minimal list of transfers that would settle the group.")
    public BalanceReport fetchGroupBalances(
            @ToolParam(description = "Id of the group being asked about") Long groupId) {
        requireAuthorizedGroup(groupId);
        Map<Long, BigDecimal> balances = ledgerService.calculateBalances(authorizedGroupId);
        List<MemberBalance> memberBalances = balances.entrySet().stream()
                .map(e -> new MemberBalance(emailOf(e.getKey()), e.getValue()))
                .toList();
        List<Transfer> transfers = SettlementCalculator.settle(balances).stream()
                .map(s -> new Transfer(emailOf(s.fromUserId()), emailOf(s.toUserId()), s.amount()))
                .toList();
        return new BalanceReport(authorizedGroupId, memberBalances, transfers);
    }

    private LedgerRow toRow(LedgerTransaction t) {
        return new LedgerRow(t.getId(), t.getDate().toString(), t.getDescription(), t.getTotalAmount(), emailOf(t.getPayerId()));
    }

    private String emailOf(Long userId) {
        return memberEmails.getOrDefault(userId, "user #" + userId);
    }

    private void requireAuthorizedGroup(Long groupId) {
        if (groupId == null || groupId != authorizedGroupId) {
            throw new IllegalArgumentException(
                    "Access denied: this conversation may only read group " + authorizedGroupId);
        }
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException(field + " must be an ISO date (yyyy-MM-dd), got: " + value);
        }
    }
}
