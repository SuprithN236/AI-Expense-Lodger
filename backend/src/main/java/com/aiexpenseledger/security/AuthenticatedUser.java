package com.aiexpenseledger.security;

/** The principal placed in the SecurityContext, built solely from verified JWT claims. */
public record AuthenticatedUser(Long id, String email) {
}
