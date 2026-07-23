package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamCandidateJpaEntity;

public interface SpringDataExamCandidateRepository extends JpaRepository<ExamCandidateJpaEntity, UUID> {
    List<ExamCandidateJpaEntity> findByExamId(UUID examId);
    long countByExamId(UUID examId);
    boolean existsByExamIdAndStudentId(UUID examId, UUID studentId);

    @Query("SELECT c.studentId FROM ExamCandidateJpaEntity c WHERE c.examId = :examId")
    List<UUID> findStudentIdsByExamId(UUID examId);
    List<ExamCandidateJpaEntity> findByExamIdAndScheduleIdIsNullOrderByAssignedAtAsc(UUID examId);
    List<ExamCandidateJpaEntity> findByIdInAndExamId(Collection<UUID> ids, UUID examId);
    long countByScheduleId(UUID scheduleId);
    boolean existsByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);
    boolean existsByExamIdAndScheduleIdIsNotNull(UUID examId);
    Optional<ExamCandidateJpaEntity> findByScheduleIdAndStudentId(UUID scheduleId, UUID studentId);

    @Query("""
        SELECT c 
            FROM ExamCandidateJpaEntity c 
        JOIN ExamScheduleJpaEntity s 
            ON c.scheduleId = s.id 
        WHERE c.studentId = :userId 
            AND s.startDate <= :now 
            AND s.endDate > :now
    """)
    List<ExamCandidateJpaEntity> findActiveCandidate(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}

