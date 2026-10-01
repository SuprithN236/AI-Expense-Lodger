package com.aiexpenseledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One immutable row of the group ledger. Rows are never updated or deleted (enforced by
 * {@link Immutable} and by database triggers); a correction is a new row whose
 * {@code reversesTransactionId} points at the original and whose amounts are negated.
 */
@Entity
@Immutable
@Table(name = "ledger_transactions")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, updatable = false)
    private Long groupId;

    @Column(nullable = false, updatable = false)
    private String description;

    @Column(name = "total_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payer_id", nullable = false, updatable = false)
    private Long payerId;

    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDate date;

    @Column(name = "idempotency_key", nullable = false, updatable = false, unique = true)
    private UUID idempotencyKey;

    @Column(name = "reverses_transaction_id", updatable = false, unique = true)
    private Long reversesTransactionId;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LedgerTransaction() {
    }

    private LedgerTransaction(Long groupId, String description, BigDecimal totalAmount, Long payerId,
                              LocalDate date, UUID idempotencyKey, Long reversesTransactionId, Long createdBy) {
        this.groupId = groupId;
        this.description = description;
        this.totalAmount = totalAmount;
        this.payerId = payerId;
        this.date = date;
        this.idempotencyKey = idempotencyKey;
        this.reversesTransactionId = reversesTransactionId;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public static LedgerTransaction expense(Long groupId, String description, BigDecimal totalAmount, Long payerId,
                                            LocalDate date, UUID idempotencyKey, Long createdBy) {
        return new LedgerTransaction(groupId, description, totalAmount, payerId, date, idempotencyKey, null, createdBy);
    }

    public static LedgerTransaction reversalOf(LedgerTransaction original, String description, LocalDate date,
                                               UUID idempotencyKey, Long createdBy) {
        return new LedgerTransaction(original.groupId, description, original.totalAmount.negate(), original.payerId,
                date, idempotencyKey, original.id, createdBy);
    }

    public boolean isReversal() {
        return reversesTransactionId != null;
    }

    public Long getId() {
        return id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Long getPayerId() {
        return payerId;
    }

    public LocalDate getDate() {
        return date;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getReversesTransactionId() {
        return reversesTransactionId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
