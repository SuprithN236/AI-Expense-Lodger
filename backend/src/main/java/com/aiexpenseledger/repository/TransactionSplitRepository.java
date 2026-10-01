package com.aiexpenseledger.repository;

import com.aiexpenseledger.domain.TransactionSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {

    List<TransactionSplit> findByTransactionId(Long transactionId);

    List<TransactionSplit> findByTransactionIdIn(Collection<Long> transactionIds);

    @Query("""
            select s from TransactionSplit s
            where s.transactionId in (select t.id from LedgerTransaction t where t.groupId = :groupId)
            """)
    List<TransactionSplit> findAllByGroupId(@Param("groupId") Long groupId);
}
