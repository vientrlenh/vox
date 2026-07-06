package com.sep.vox.infrastructure.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}

