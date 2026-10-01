package com.aiexpenseledger.exception;

/** A ledger write reused an idempotency key (or reversed an entry twice) and was rejected. */
public class DuplicateSubmissionException extends RuntimeException {

    private final Long existingTransactionId;

    public DuplicateSubmissionException(String message, Long existingTransactionId) {
        super(message);
        this.existingTransactionId = existingTransactionId;
    }

    public Long getExistingTransactionId() {
        return existingTransactionId;
    }
}
