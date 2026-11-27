package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.registerform.RegisterFormJpaEntity;

import jakarta.persistence.LockModeType;

public interface SpringDataRegisterFormRepository extends JpaRepository<RegisterFormJpaEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegisterFormJpaEntity r WHERE r.id = :id")
    Optional<RegisterFormJpaEntity> findByIdForUpdate(@Param("id") UUID id);

    @Modifying
    @Query("""
        UPDATE RegisterFormJpaEntity r 
        SET r.status = 'APPROVED', 
            r.updatedAt = :now, 
            r.reviewedBy = :updatedBy 
        WHERE r.id = :id 
        AND r.status = 'PENDING'
            """)
    int updateApprovedRegisterForm(@Param("id") UUID id, @Param("updatedBy") UUID updatedBy, @Param("now") Instant now);

    @Modifying
    @Query("""
        UPDATE RegisterFormJpaEntity r 
        SET r.updatedAt = :now, 
            r.status = 'REJECTED',
            r.rejectedReason = :reason,  
            r.reviewedBy = :updatedBy 
        WHERE r.id = :id 
        AND r.status = 'PENDING'
    """)
    int updateRejectedRegisterForm(@Param("id") UUID id, @Param("updatedBy") UUID updatedBy, @Param("reason") String reason, @Param("now") Instant now);

    boolean existsBySchoolDirectoryIdAndStatusIn(UUID schoolDirectoryId, Collection<String> statuses);
    boolean existsByContactEmailAndStatus(String contactEmail, String status);
    boolean existsByContactPhoneAndStatus(String contactPhone, String status);

    long countByStatus(String status);
    long countByCreatedAtAfter(Instant after);

    /**
     * {@code MIN} instead of {@code findFirstBy...OrderBy...} returning the whole entity: the caller only needs a timestamp,
     * and loading the entire registration form with every column just to read {@code createdAt} is wasteful.
     *
     * @return null when no form remains in this status
     */
    @Query("SELECT MIN(r.createdAt) FROM RegisterFormJpaEntity r WHERE r.status = :status")
    Instant findOldestCreatedAtByStatus(@Param("status") String status);
}
