package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamItemResponseJpaEntity;

public interface SpringDataExamItemResponseRepository extends JpaRepository<ExamItemResponseJpaEntity, UUID> {

    @Query("""
        SELECT r, s.examId FROM ExamItemResponseJpaEntity r
        JOIN ExamSessionJpaEntity s ON s.id = r.sessionId
        WHERE r.audioUrl IS NOT NULL
        AND s.candidateId IN (
            SELECT c.id FROM ExamCandidateJpaEntity c WHERE c.studentId = :studentId
        )
        ORDER BY r.submittedAt DESC
        """)
    List<Object[]> findByStudentIdWithAudio(@Param("studentId") UUID studentId);
}
