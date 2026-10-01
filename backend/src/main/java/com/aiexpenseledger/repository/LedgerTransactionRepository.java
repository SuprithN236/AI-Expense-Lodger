package com.aiexpenseledger.repository;

import com.aiexpenseledger.domain.LedgerTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    List<LedgerTransaction> findByGroupId(Long groupId);

    List<LedgerTransaction> findByGroupIdOrderByDateDescIdDesc(Long groupId, Pageable pageable);

    Optional<LedgerTransaction> findByIdAndGroupId(Long id, Long groupId);

    Optional<LedgerTransaction> findByIdempotencyKey(UUID idempotencyKey);

    Optional<LedgerTransaction> findByReversesTransactionId(Long reversesTransactionId);

    List<LedgerTransaction> findByReversesTransactionIdIn(List<Long> reversesTransactionIds);

    // Reversals carry negative amounts, so plain sums already net out corrected entries.

    @Query("select coalesce(sum(t.totalAmount), 0) from LedgerTransaction t where t.groupId = :groupId")
    BigDecimal sumTotalByGroupId(@Param("groupId") Long groupId);

    @Query("""
            select t from LedgerTransaction t
            where t.groupId = :groupId and t.date between :startDate and :endDate
            order by t.date desc, t.id desc
            """)
    List<LedgerTransaction> findBetween(@Param("groupId") Long groupId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
