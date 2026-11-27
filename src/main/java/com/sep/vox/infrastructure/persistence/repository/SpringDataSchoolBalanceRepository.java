package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.school.SchoolBalanceJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataSchoolBalanceRepository extends JpaRepository<SchoolBalanceJpaEntity, UUID> {

    Optional<SchoolBalanceJpaEntity> findBySchoolId(UUID schoolId);

    // PESSIMISTIC_WRITE: blocks every other transaction touching this school's balance until the
    // current transaction commits. Required because the accompanying entry must record balance_after_vnd -- reading
    // without a lock lets two parallel top-ups/deductions write two entries with the same balanceAfter.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SchoolBalanceJpaEntity> findWithLockBySchoolId(UUID schoolId);

    /**
     * Ensures the school HAS a balance row, so {@link #findWithLockBySchoolId} has something to lock.
     *
     * <p>SELECT ... FOR UPDATE cannot lock a row that does not exist, so "read, and if not found create
     * a new one" is a real race window: two parallel gradings for a school that has NEVER topped up both
     * see nothing and both INSERT, then one dies on uq_school_balances_school. That is also exactly
     * the most common case -- most schools never top up, and the first time they touch the balance
     * is the first time spending exceeds the quota.
     *
     * <p>ON CONFLICT DO NOTHING turns that window into a wait: the later transaction blocks here
     * until the earlier one commits/rolls back, then does nothing and moves on -- the lock right after
     * reads exactly the committed row.
     *
     * <p>The id is left to Postgres (DEFAULT uuidv7()) and version = 0 so the entity's @Version is in step
     * at the next save.
     */
    @Modifying
    @Query(value = """
        INSERT INTO school_balances (school_id, balance_vnd, created_at, updated_at, version)
        VALUES (:schoolId, 0, :now, :now, 0)
        ON CONFLICT (school_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("schoolId") UUID schoolId, @Param("now") Instant now);
}
