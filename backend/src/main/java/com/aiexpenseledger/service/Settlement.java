package com.aiexpenseledger.service;

import java.math.BigDecimal;

/** "fromUserId should pay toUserId this amount" — one edge of a simplified debt graph. */
public record Settlement(Long fromUserId, Long toUserId, BigDecimal amount) {
}
