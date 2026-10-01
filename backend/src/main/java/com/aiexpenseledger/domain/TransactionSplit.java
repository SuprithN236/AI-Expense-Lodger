package com.aiexpenseledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/** The share of one {@link LedgerTransaction} that a single user owes. Immutable like its parent. */
@Entity
@Immutable
@Table(name = "transaction_splits")
public class TransactionSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private Long transactionId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "owed_amount", nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal owedAmount;

    protected TransactionSplit() {
    }

    public TransactionSplit(Long transactionId, Long userId, BigDecimal owedAmount) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.owedAmount = owedAmount;
    }

    public Long getId() {
        return id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getOwedAmount() {
        return owedAmount;
    }
}
