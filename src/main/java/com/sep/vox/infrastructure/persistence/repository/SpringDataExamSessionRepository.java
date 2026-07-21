package com.sep.vox.infrastructure.persistence.repository;

import com.sep.vox.infrastructure.persistence.entity.ExamSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataExamSessionRepository extends JpaRepository<ExamSessionJpaEntity, UUID> {
    Optional<ExamSessionJpaEntity> findByExamIdAndCandidateIdAndStatus(UUID examId, UUID candidateId, String status);
    Optional<ExamSessionJpaEntity> findByIdAndStatus(UUID id, String status);

    @Query("""
        SELECT s 
        FROM ExamSessionJpaEntity s 
        JOIN ExamJpaEntity e 
            ON s.examId = e.id 
        WHERE s.id IN :ids
            AND s.startedAt <= :now 
            AND s.submittedAt IS NULL 
            AND s.status = 'IN_PROGRESS' 
            AND e.schoolId = :schoolId
    """)
    List<ExamSessionJpaEntity> findActiveByIdInAndSchoolId(@Param("ids") Collection<UUID> ids, @Param("now") OffsetDateTime now, @Param("schoolId") UUID schoolId);
}
