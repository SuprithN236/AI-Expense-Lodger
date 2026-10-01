package com.aiexpenseledger.web;

import com.aiexpenseledger.domain.Group;
import com.aiexpenseledger.domain.LedgerTransaction;
import com.aiexpenseledger.domain.User;
import com.aiexpenseledger.monitoring.LogMonitoringClient;
import com.aiexpenseledger.security.AuthenticatedUser;
import com.aiexpenseledger.service.GroupService;
import com.aiexpenseledger.service.LedgerService;
import com.aiexpenseledger.service.LedgerService.LedgerEntry;
import com.aiexpenseledger.service.Money;
import com.aiexpenseledger.service.SettlementCalculator;
import com.aiexpenseledger.web.dto.BalanceSheetResponse;
import com.aiexpenseledger.web.dto.BalanceSheetResponse.MemberBalance;
import com.aiexpenseledger.web.dto.BalanceSheetResponse.SettlementResponse;
import com.aiexpenseledger.web.dto.LogExpenseRequest;
import com.aiexpenseledger.web.dto.ReverseTransactionRequest;
import com.aiexpenseledger.web.dto.TransactionResponse;
import com.aiexpenseledger.web.dto.TransactionResponse.SplitResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ledger endpoints. The ledger is append-only, so "update" and "delete" are expressed as a reversal:
 * a new entry that cancels the original out.
 */
@Validated
@RestController
@RequestMapping("/api/v1/groups/{groupId}")
public class LedgerController {

    private final LedgerService ledgerService;
    private final GroupService groupService;
    private final LogMonitoringClient monitoring;

    public LedgerController(LedgerService ledgerService, GroupService groupService, LogMonitoringClient monitoring) {
        this.ledgerService = ledgerService;
        this.groupService = groupService;
        this.monitoring = monitoring;
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse logExpense(@AuthenticationPrincipal AuthenticatedUser user,
                                          @PathVariable Long groupId,
                                          @Valid @RequestBody LogExpenseRequest request) {
        Long payerId = request.payerId() != null ? request.payerId() : user.id();
        Map<Long, BigDecimal> splits = request.splits() != null && !request.splits().isEmpty()
                ? request.splits()
                : Money.equalSplit(request.totalAmount(), request.participantIds());

        LedgerEntry entry = ledgerService.logExpense(groupId, payerId, request.totalAmount(), splits,
                request.description(), request.date(), request.idempotencyKey(), user.id());
        monitoring.info(LogMonitoringClient.SERVICE_LEDGER, "Expense " + entry.transaction().getId()
                + " recorded in group " + groupId + " by user " + user.id());
        return toResponse(entry, memberEmails(groupId, user.id()));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> listTransactions(@AuthenticationPrincipal AuthenticatedUser user,
                                                      @PathVariable Long groupId,
                                                      @RequestParam(defaultValue = "100") @Min(1) @Max(500) int limit) {
        Map<Long, String> emails = memberEmails(groupId, user.id());
        return ledgerService.listTransactions(groupId, limit).stream()
                .map(entry -> toResponse(entry, emails))
                .toList();
    }

    @GetMapping("/balances")
    public BalanceSheetResponse getBalances(@AuthenticationPrincipal AuthenticatedUser user,
                                            @PathVariable Long groupId) {
        Map<Long, String> emails = memberEmails(groupId, user.id());
        Map<Long, BigDecimal> balances = ledgerService.calculateBalances(groupId);

        List<MemberBalance> memberBalances = balances.entrySet().stream()
                .map(e -> new MemberBalance(e.getKey(), emails.get(e.getKey()), e.getValue()))
                .toList();
        List<SettlementResponse> settlements = SettlementCalculator.settle(balances).stream()
                .map(s -> new SettlementResponse(s.fromUserId(), emails.get(s.fromUserId()),
                        s.toUserId(), emails.get(s.toUserId()), s.amount()))
                .toList();
        return new BalanceSheetResponse(groupId, ledgerService.fetchGroupTotalSpent(groupId), memberBalances, settlements);
    }

    @PostMapping("/transactions/{transactionId}/reversal")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse reverseTransaction(@AuthenticationPrincipal AuthenticatedUser user,
                                                  @PathVariable Long groupId,
                                                  @PathVariable Long transactionId,
                                                  @Valid @RequestBody ReverseTransactionRequest request) {
        LedgerEntry entry = ledgerService.reverseTransaction(groupId, transactionId, request.idempotencyKey(), user.id());
        monitoring.info(LogMonitoringClient.SERVICE_LEDGER, "Transaction " + transactionId + " reversed by entry "
                + entry.transaction().getId() + " in group " + groupId + " by user " + user.id());
        return toResponse(entry, memberEmails(groupId, user.id()));
    }

    /** Also serves as the membership check for every read endpoint. */
    private Map<Long, String> memberEmails(Long groupId, Long userId) {
        Group group = groupService.getGroupForMember(groupId, userId);
        return group.getMembers().stream().collect(Collectors.toMap(User::getId, User::getEmail));
    }

    private static TransactionResponse toResponse(LedgerEntry entry, Map<Long, String> emails) {
        LedgerTransaction t = entry.transaction();
        List<SplitResponse> splits = entry.splits().stream()
                .map(s -> new SplitResponse(s.getUserId(), emails.get(s.getUserId()), s.getOwedAmount()))
                .toList();
        return new TransactionResponse(t.getId(), t.getGroupId(), t.getDescription(), t.getTotalAmount(),
                t.getPayerId(), emails.get(t.getPayerId()), t.getDate(), t.getCreatedBy(), t.getCreatedAt(),
                t.getReversesTransactionId(), entry.reversedByTransactionId(), splits);
    }
}
