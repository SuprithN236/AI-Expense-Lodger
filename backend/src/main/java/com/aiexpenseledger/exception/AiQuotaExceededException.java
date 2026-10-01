package com.aiexpenseledger.exception;

/** A daily AI usage cap was reached; the request was not sent to the AI provider. */
public class AiQuotaExceededException extends RuntimeException {

    public AiQuotaExceededException(String message) {
        super(message);
    }
}
