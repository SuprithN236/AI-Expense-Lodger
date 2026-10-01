package com.aiexpenseledger.repository;

import com.aiexpenseledger.domain.Group;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("""
            select distinct g from ExpenseGroup g
            left join fetch g.members
            where g.id in (select g2.id from ExpenseGroup g2 join g2.members m where m.id = :userId)
            order by g.name
            """)
    List<Group> findAllByMemberIdWithMembers(@Param("userId") Long userId);

    @EntityGraph(attributePaths = "members")
    @Query("select g from ExpenseGroup g where g.id = :groupId")
    Optional<Group> findByIdWithMembers(@Param("groupId") Long groupId);

    @Query("""
            select count(g) > 0 from ExpenseGroup g join g.members m
            where g.id = :groupId and m.id = :userId
            """)
    boolean isMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
