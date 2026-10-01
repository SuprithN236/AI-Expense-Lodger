package com.aiexpenseledger.service;

import com.aiexpenseledger.domain.Group;
import com.aiexpenseledger.domain.LedgerTransaction;
import com.aiexpenseledger.domain.TransactionSplit;
import com.aiexpenseledger.domain.User;
import com.aiexpenseledger.exception.DuplicateSubmissionException;
import com.aiexpenseledger.exception.LedgerValidationException;
import com.aiexpenseledger.exception.ResourceNotFoundException;
import com.aiexpenseledger.repository.GroupRepository;
import com.aiexpenseledger.repository.LedgerTransactionRepository;
import com.aiexpenseledger.repository.TransactionSplitRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The only component allowed to write to the ledger. Every write appends rows; balances are always
 * recomputed from the full history rather than read from a stored running total.
 */
@Service
public class LedgerService {

    public static final int MAX_DESCRIPTION_LENGTH = 255;
    private static final String REVERSAL_PREFIX = "Reversal: ";

    private final LedgerTransactionRepository transactionRepository;
    private final TransactionSplitRepository splitRepository;
    private final GroupRepository groupRepository;

    public LedgerService(LedgerTransactionRepository transactionRepository,
                         TransactionSplitRepository splitRepository,
                         GroupRepository groupRepository) {
        this.transactionRepository = transactionRepository;
        this.splitRepository = splitRepository;
        this.groupRepository = groupRepository;
    }

    /** A ledger row with its splits, and the id of the row that reversed it (if any). */
    public record LedgerEntry(LedgerTransaction transaction, List<TransactionSplit> splits, Long reversedByTransactionId) {
    }

    /** Aggregated, read-only spending figures exposed to the AI assistant. */
    public record SpendingSummary(Long groupId, LocalDate startDate, LocalDate endDate, List<String> keywords,
                                  BigDecimal totalSpent, int transactionCount, List<LedgerTransaction> transactions) {
    }

    // ------------------------------------------------------------------------------------------------
    // Writes
    // ------------------------------------------------------------------------------------------------

    /**
     * Atomically appends one expense and its splits. Either every row is written or none is.
     *
     * @param splits         user id → amount that user owes; must sum exactly to {@code totalAmount}
     * @param idempotencyKey client-generated key; a second request with the same key is rejected
     * @param actorId        the authenticated user recording the expense (must be a group member)
     */
    @Transactional
    public LedgerEntry logExpense(Long groupId, Long payerId, BigDecimal totalAmount, Map<Long, BigDecimal> splits,
                                  String description, LocalDate date, UUID idempotencyKey, Long actorId) {
        if (idempotencyKey == null) {
            throw new LedgerValidationException("idempotencyKey is required");
        }
        rejectIfKeyUsed(idempotencyKey);

        Group group = loadGroup(groupId);
        Set<Long> memberIds = group.getMembers().stream().map(User::getId).collect(Collectors.toSet());
        requireMember(memberIds, actorId, "You are not a member of this group");
        requireMember(memberIds, payerId, "The payer must be a member of the group");

        String cleanDescription = cleanDescription(description);
        BigDecimal total = Money.normalize(totalAmount, "totalAmount");
        if (total.signum() <= 0) {
            throw new LedgerValidationException("totalAmount must be greater than zero");
        }
        Map<Long, BigDecimal> normalizedSplits = validateSplits(splits, total, memberIds);

        LedgerTransaction transaction = saveTransaction(LedgerTransaction.expense(
                groupId, cleanDescription, total, payerId, date != null ? date : LocalDate.now(), idempotencyKey, actorId));

        List<TransactionSplit> savedSplits = splitRepository.saveAll(normalizedSplits.entrySet().stream()
                .map(e -> new TransactionSplit(transaction.getId(), e.getKey(), e.getValue()))
                .toList());
        return new LedgerEntry(transaction, savedSplits, null);
    }

    /**
     * Corrects a mistaken entry without rewriting history: appends a mirror transaction whose total and
     * splits are negated, so the pair nets to zero in every balance.
     */
    @Transactional
    public LedgerEntry reverseTransaction(Long groupId, Long transactionId, UUID idempotencyKey, Long actorId) {
        if (idempotencyKey == null) {
            throw new LedgerValidationException("idempotencyKey is required");
        }
        rejectIfKeyUsed(idempotencyKey);

        Group group = loadGroup(groupId);
        requireMember(group.getMembers().stream().map(User::getId).collect(Collectors.toSet()),
                actorId, "You are not a member of this group");

        LedgerTransaction original = transactionRepository.findByIdAndGroupId(transactionId, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction " + transactionId + " not found in group " + groupId));
        if (original.isReversal()) {
            throw new LedgerValidationException("A reversal entry cannot itself be reversed");
        }
        transactionRepository.findByReversesTransactionId(transactionId).ifPresent(existing -> {
            throw new DuplicateSubmissionException("Transaction " + transactionId + " has already been reversed", existing.getId());
        });

        String description = truncate(REVERSAL_PREFIX + original.getDescription());
        LedgerTransaction reversal = saveTransaction(
                LedgerTransaction.reversalOf(original, description, LocalDate.now(), idempotencyKey, actorId));

        List<TransactionSplit> reversedSplits = splitRepository.saveAll(splitRepository.findByTransactionId(transactionId).stream()
                .map(split -> new TransactionSplit(reversal.getId(), split.getUserId(), split.getOwedAmount().negate()))
                .toList());
        return new LedgerEntry(reversal, reversedSplits, null);
    }

    // ------------------------------------------------------------------------------------------------
    // Reads (callers are responsible for checking group membership first)
    // ------------------------------------------------------------------------------------------------

    /**
     * Derives every member's net position from the immutable history:
     * net = (sum of totals the user paid) − (sum of split amounts the user owes).
     * Positive means the group owes the user; negative means the user owes the group. All nets sum to zero.
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> calculateBalances(Long groupId) {
        Group group = loadGroup(groupId);
        Map<Long, BigDecimal> balances = new TreeMap<>();
        group.getMembers().forEach(member -> balances.put(member.getId(), Money.ZERO));

        for (LedgerTransaction transaction : transactionRepository.findByGroupId(groupId)) {
            balances.merge(transaction.getPayerId(), transaction.getTotalAmount(), BigDecimal::add);
        }
        for (TransactionSplit split : splitRepository.findAllByGroupId(groupId)) {
            balances.merge(split.getUserId(), split.getOwedAmount().negate(), BigDecimal::add);
        }
        balances.replaceAll((userId, net) -> net.setScale(Money.SCALE));
        return balances;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> listTransactions(Long groupId, int limit) {
        List<LedgerTransaction> transactions =
                transactionRepository.findByGroupIdOrderByDateDescIdDesc(groupId, PageRequest.of(0, limit));
        if (transactions.isEmpty()) {
            return List.of();
        }
        List<Long> ids = transactions.stream().map(LedgerTransaction::getId).toList();
        Map<Long, List<TransactionSplit>> splitsByTransaction = splitRepository.findByTransactionIdIn(ids).stream()
                .sorted(Comparator.comparing(TransactionSplit::getUserId))
                .collect(Collectors.groupingBy(TransactionSplit::getTransactionId));
        Map<Long, Long> reversedBy = transactionRepository.findByReversesTransactionIdIn(ids).stream()
                .collect(Collectors.toMap(LedgerTransaction::getReversesTransactionId, LedgerTransaction::getId));

        return transactions.stream()
                .map(t -> new LedgerEntry(t, splitsByTransaction.getOrDefault(t.getId(), List.of()), reversedBy.get(t.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal fetchGroupTotalSpent(Long groupId) {
        return transactionRepository.sumTotalByGroupId(groupId).setScale(Money.SCALE);
    }

    /**
     * Totals spending in an inclusive date range, optionally restricted to entries whose description contains
     * any of {@code keywords} (case-insensitive). Matching happens in Java, never by building SQL from input,
     * and an entry matching several keywords is counted once.
     */
    @Transactional(readOnly = true)
    public SpendingSummary summarizeSpending(Long groupId, LocalDate startDate, LocalDate endDate,
                                             List<String> keywords, int maxTransactions) {
        if (startDate.isAfter(endDate)) {
            throw new LedgerValidationException("startDate must be on or before endDate");
        }
        List<String> normalizedKeywords = keywords == null ? List.of() : keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(k -> k.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();

        List<LedgerTransaction> matching = transactionRepository.findBetween(groupId, startDate, endDate).stream()
                .filter(t -> normalizedKeywords.isEmpty()
                        || normalizedKeywords.stream().anyMatch(t.getDescription().toLowerCase(Locale.ROOT)::contains))
                .toList();
        BigDecimal total = matching.stream()
                .map(LedgerTransaction::getTotalAmount)
                .reduce(Money.ZERO, BigDecimal::add);
        return new SpendingSummary(groupId, startDate, endDate, normalizedKeywords, total, matching.size(),
                matching.stream().limit(maxTransactions).toList());
    }

    // ------------------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------------------

    private Group loadGroup(Long groupId) {
        return groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group " + groupId + " not found"));
    }

    private void rejectIfKeyUsed(UUID idempotencyKey) {
        transactionRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            throw new DuplicateSubmissionException(
                    "This submission was already recorded as transaction " + existing.getId(), existing.getId());
        });
    }

    /**
     * Flushes immediately so a concurrent duplicate (two requests racing past {@link #rejectIfKeyUsed}) hits the
     * database unique constraints inside this method and is reported as a duplicate, not a generic error.
     */
    private LedgerTransaction saveTransaction(LedgerTransaction transaction) {
        try {
            return transactionRepository.saveAndFlush(transaction);
        } catch (DataIntegrityViolationException e) {
            String message = String.valueOf(e.getMostSpecificCause().getMessage());
            if (message.contains("uk_ledger_idempotency_key")) {
                throw new DuplicateSubmissionException("This submission was already recorded", null);
            }
            if (message.contains("uk_ledger_reverses_transaction")) {
                throw new DuplicateSubmissionException("This transaction has already been reversed", null);
            }
            throw e;
        }
    }

    private static Map<Long, BigDecimal> validateSplits(Map<Long, BigDecimal> splits, BigDecimal total, Set<Long> memberIds) {
        if (splits == null || splits.isEmpty()) {
            throw new LedgerValidationException("At least one split is required");
        }
        Map<Long, BigDecimal> normalized = new TreeMap<>();
        for (Map.Entry<Long, BigDecimal> entry : splits.entrySet()) {
            requireMember(memberIds, entry.getKey(), "Split user " + entry.getKey() + " is not a member of the group");
            BigDecimal owed = Money.normalize(entry.getValue(), "Split amount for user " + entry.getKey());
            if (owed.signum() < 0) {
                throw new LedgerValidationException("Split amounts cannot be negative");
            }
            normalized.put(entry.getKey(), owed);
        }
        BigDecimal splitSum = normalized.values().stream().reduce(Money.ZERO, BigDecimal::add);
        if (splitSum.compareTo(total) != 0) {
            throw new LedgerValidationException(
                    "Splits sum to " + splitSum.toPlainString() + " but totalAmount is " + total.toPlainString());
        }
        normalized.values().removeIf(amount -> amount.signum() == 0);
        return normalized;
    }

    private static void requireMember(Set<Long> memberIds, Long userId, String message) {
        if (userId == null || !memberIds.contains(userId)) {
            throw new LedgerValidationException(message);
        }
    }

    private static String cleanDescription(String description) {
        String trimmed = description == null ? "" : description.trim();
        if (trimmed.isEmpty()) {
            throw new LedgerValidationException("description is required");
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new LedgerValidationException("description must be at most " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return trimmed;
    }

    private static String truncate(String value) {
        return value.length() <= MAX_DESCRIPTION_LENGTH ? value : value.substring(0, MAX_DESCRIPTION_LENGTH);
    }
}
