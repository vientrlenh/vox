package com.sep.vox.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamResultAppealJpaEntity;

public interface SpringDataExamResultAppealRepository extends JpaRepository<ExamResultAppealJpaEntity, UUID> {

    /** Đơn còn đang xử lý = chưa PUBLISHED và chưa REJECTED. Dùng để chặn nộp đơn trùng. */
    @Query("""
        SELECT COUNT(a) > 0 FROM ExamResultAppealJpaEntity a
        WHERE a.candidateResultId = :candidateResultId
        AND a.status NOT IN ('PUBLISHED', 'REJECTED')
    """)
    boolean existsOpenByCandidateResultId(@Param("candidateResultId") UUID candidateResultId);

    List<ExamResultAppealJpaEntity> findByCandidateResultId(UUID candidateResultId);
}
