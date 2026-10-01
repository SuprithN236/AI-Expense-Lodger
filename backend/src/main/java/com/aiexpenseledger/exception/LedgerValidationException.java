package com.aiexpenseledger.exception;

/** A ledger write was rejected because it would violate a business rule (e.g. splits not summing). */
public class LedgerValidationException extends RuntimeException {

    public LedgerValidationException(String message) {
        super(message);
    }
}
