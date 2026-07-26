package com.sep.vox.infrastructure.persistence.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sep.vox.infrastructure.persistence.entity.ExamGradingAssignmentJpaEntity;

public interface SpringDataExamGradingAssignmentRepository
        extends JpaRepository<ExamGradingAssignmentJpaEntity, UUID> {

    /**
     * Dòng đang mở của một bài. Lọc bằng {@code active_result_id} chứ không bằng
     * {@code candidate_result_id + status}: cột active chính là thứ unique index đang
     * canh, nên hai chỗ luôn nói cùng một chuyện.
     */
    Optional<ExamGradingAssignmentJpaEntity> findByActiveResultId(UUID activeResultId);

    List<ExamGradingAssignmentJpaEntity> findByActiveResultIdIn(Collection<UUID> activeResultIds);

    List<ExamGradingAssignmentJpaEntity> findByCandidateResultIdOrderByAssignedAtDesc(UUID candidateResultId);

    List<ExamGradingAssignmentJpaEntity> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    List<ExamGradingAssignmentJpaEntity> findByAppealId(UUID appealId);

    @Query("""
        SELECT ga FROM ExamGradingAssignmentJpaEntity ga
        WHERE ga.status = 'ASSIGNED' AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :now
        ORDER BY ga.deadlineAt ASC
    """)
    List<ExamGradingAssignmentJpaEntity> findOverdue(@Param("now") OffsetDateTime now);

    /** Sắp/đã tới hạn mà chưa nhắc lần nào — {@code reminded_at} là chốt chống trùng. */
    @Query("""
        SELECT ga FROM ExamGradingAssignmentJpaEntity ga
        WHERE ga.status = 'ASSIGNED' AND ga.remindedAt IS NULL
        AND ga.deadlineAt IS NOT NULL AND ga.deadlineAt < :threshold
        ORDER BY ga.deadlineAt ASC
    """)
    List<ExamGradingAssignmentJpaEntity> findDueForReminder(@Param("threshold") OffsetDateTime threshold);

    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}
