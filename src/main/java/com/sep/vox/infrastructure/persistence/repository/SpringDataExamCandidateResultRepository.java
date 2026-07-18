package com.sep.vox.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamCandidateResultJpaEntity;

public interface SpringDataExamCandidateResultRepository extends JpaRepository<ExamCandidateResultJpaEntity, UUID> {

    @Query("""
        SELECT r FROM ExamCandidateResultJpaEntity r
        WHERE r.candidateId IN (
            SELECT c.id FROM ExamCandidateJpaEntity c WHERE c.studentId = :studentId
        )
        ORDER BY r.createdAt DESC
        """)
    Page<ExamCandidateResultJpaEntity> findByStudentId(@Param("studentId") UUID studentId, Pageable pageable);
}
